package com.example.auction.service;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.model.BidHistory;
import com.example.auction.model.User;
import com.example.auction.repository.AuctionItemRepository;
import com.example.auction.repository.BidHistoryRepository;
import com.example.auction.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for handling core auction and bidding logic.
 */
/**
 * Service class for handling core auction and bidding logic.
 */
@Service
@Slf4j
public class AuctionService {

    private final AuctionItemRepository auctionItemRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // The magic WS wand
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public AuctionService(AuctionItemRepository auctionItemRepository,
            BidHistoryRepository bidHistoryRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate,
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.auctionItemRepository = auctionItemRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves all auction items.
     *
     * @return List of AuctionItem.
     */
    /**
     * Retrieves all auction items.
     *
     * @return List of AuctionItem.
     */
    public List<AuctionItem> getAllAuctions() {
        log.debug("Retrieving all auction items");
        log.debug("Retrieving all auction items");
        return auctionItemRepository.findAll();
    }

    /**
     * Retrieves a specific auction item by its ID.
     *
     * @param id The ID of the auction item.
     * @return The AuctionItem.
     * @throws RuntimeException if the item is not found.
     */
    /**
     * Retrieves a specific auction item by its ID.
     *
     * @param id The ID of the auction item.
     * @return The AuctionItem.
     * @throws RuntimeException if the item is not found.
     */
    public AuctionItem getAuctionById(Long id) {
        log.debug("Retrieving auction item with ID: {}", id);
        log.debug("Retrieving auction item with ID: {}", id);
        return auctionItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Auction item with ID: {} not found", id);
                    return new RuntimeException("Auction item not found");
                });
                .orElseThrow(() -> {
                    log.error("Auction item with ID: {} not found", id);
                    return new RuntimeException("Auction item not found");
                });
    }

    /**
     * Places a bid on a specific auction item.
     * Uses pessimistic or optimistic locking strategy configured on the repository level
     * to manage concurrent bid requests.
     *
     * @param auctionId  The ID of the auction item.
     * @param bidRequest The requested bid details.
     * @param username   The username of the bidder.
     * @return BidResponse indicating the result of the bid attempt.
     */
    @Transactional(readOnly = true)
    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, String username) {
        log.info("Attempting to place a bid of {} by user '{}' on auction ID: {}", bidRequest.getAmount(), username, auctionId);
        log.info("Attempting to place a bid of {} by user '{}' on auction ID: {}", bidRequest.getAmount(), username, auctionId);

        AuctionItem item = auctionItemRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.error("Auction item with ID: {} not found during bid placement", auctionId);
                    return new RuntimeException("Auction item not found");
                });
                .orElseThrow(() -> {
                    log.error("Auction item with ID: {} not found during bid placement", auctionId);
                    return new RuntimeException("Auction item not found");
                });

        if (item.getEndTime().isBefore(LocalDateTime.now())) {
            log.warn("Bid rejected for auction ID: {}. Reason: Auction has ended", auctionId);
            log.warn("Bid rejected for auction ID: {}. Reason: Auction has ended", auctionId);
            return new BidResponse("REJECTED", "Auction has ended", auctionId, 
                    item.getCurrentHighestBid(), null);
        }

        // We check redis first. If it's not present, we seed it with the current highest from DB
        String redisKey = "auction:" + auctionId + ":highest_bid";
        redisTemplate.opsForValue().setIfAbsent(redisKey, 
                (item.getCurrentHighestBid() != null ? item.getCurrentHighestBid() : item.getStartingPrice()).toString());

        String luaScript = 
            "local current_bid = redis.call('get', KEYS[1]); " +
            "if (not current_bid or tonumber(ARGV[1]) > tonumber(current_bid)) then " +
            "   redis.call('set', KEYS[1], ARGV[1]); " +
            "   return 1; " +
            "else " +
            "   return 0; " +
            "end";

        Long result = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
            java.util.Collections.singletonList(redisKey),
            bidRequest.getAmount().toString()
        );

        if (result == null || result == 0L) {
            String currentRedisBid = redisTemplate.opsForValue().get(redisKey);
            BigDecimal currentBid = currentRedisBid != null ? new BigDecimal(currentRedisBid) : item.getStartingPrice();
            
            log.warn("Bid rejected for auction ID: {}. Amount {} is not higher than {}", auctionId, bidRequest.getAmount(), currentBid);
            return new BidResponse("REJECTED", 
                    "Bid must be higher than current highest bid: " + currentBid, 
                    auctionId, currentBid, null);
        }

        User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User '{}' not found during bid placement", username);
                    return new RuntimeException("User not found");
                });

        // Push to Redis Queue for async persistence
        String queueData = auctionId + ":" + bidder.getId() + ":" + bidRequest.getAmount() + ":" + LocalDateTime.now();
        redisTemplate.opsForList().leftPush("auction:bids:queue", queueData);
        log.debug("Pushed winning bid to Redis queue for auction ID: {} with amount {}", auctionId, bidRequest.getAmount());

        BidResponse response = new BidResponse("ACCEPTED", "Your bid has been placed!", 
                auctionId, bidRequest.getAmount(), username);

        // --- THE WEBSOCKET BROADCAST ---
        log.debug("Broadcasting winning bid to WebSocket topic /topic/auctions/{}", auctionId);
        messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, response);
        
        return response;
    }
}
