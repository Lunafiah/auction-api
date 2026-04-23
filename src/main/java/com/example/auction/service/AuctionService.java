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
@Service
@Slf4j
public class AuctionService {

    private final AuctionItemRepository auctionItemRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // The magic WS wand

    public AuctionService(AuctionItemRepository auctionItemRepository,
                          BidHistoryRepository bidHistoryRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.auctionItemRepository = auctionItemRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Retrieves all auction items.
     *
     * @return List of AuctionItem.
     */
    public List<AuctionItem> getAllAuctions() {
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
    public AuctionItem getAuctionById(Long id) {
        log.debug("Retrieving auction item with ID: {}", id);
        return auctionItemRepository.findById(id)
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
    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, String username) {
        log.info("Attempting to place a bid of {} by user '{}' on auction ID: {}", bidRequest.getAmount(), username, auctionId);

        AuctionItem item = auctionItemRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.error("Auction item with ID: {} not found during bid placement", auctionId);
                    return new RuntimeException("Auction item not found");
                });

        if (item.getEndTime().isBefore(LocalDateTime.now())) {
            log.warn("Bid rejected for auction ID: {}. Reason: Auction has ended", auctionId);
            return new BidResponse("REJECTED", "Auction has ended", auctionId, 
                    item.getCurrentHighestBid(), null);
        }

        BigDecimal minimumBid = item.getCurrentHighestBid() != null 
                ? item.getCurrentHighestBid() 
                : item.getStartingPrice();

        if (bidRequest.getAmount().compareTo(minimumBid) <= 0) {
            log.warn("Bid rejected for auction ID: {}. Amount {} is less than or equal to minimum required bid {}", auctionId, bidRequest.getAmount(), minimumBid);
            return new BidResponse("REJECTED", 
                    "Bid must be higher than current highest bid: " + minimumBid, 
                    auctionId, minimumBid, null);
        }

        User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User '{}' not found during bid placement", username);
                    return new RuntimeException("User not found");
                });

        item.setCurrentHighestBid(bidRequest.getAmount());
        item.setHighestBidderId(bidder.getId());

        try {
            auctionItemRepository.save(item);
            log.debug("Successfully saved auction item ID: {} with new highest bid {}", auctionId, bidRequest.getAmount());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for auction ID: {}. Another user placed a higher bid concurrently.", auctionId);
            // Because they used the POST route, we just send a clean 409 conflict back!
            AuctionItem freshItem = auctionItemRepository.findById(auctionId).orElse(item);
            return new BidResponse("REJECTED", 
                    "Someone placed a higher bid just before you! Try again.", 
                    auctionId, freshItem.getCurrentHighestBid(), null);
        }

        BidHistory history = new BidHistory();
        history.setAuctionItemId(auctionId);
        history.setUserId(bidder.getId());
        history.setBidAmount(bidRequest.getAmount());
        history.setTimestamp(LocalDateTime.now());
        bidHistoryRepository.save(history);
        
        log.info("Bid history recorded for user '{}' on auction ID: {} with amount {}", username, auctionId, bidRequest.getAmount());

        BidResponse response = new BidResponse("ACCEPTED", "Your bid has been placed!", 
                auctionId, bidRequest.getAmount(), username);

        // --- THE WEBSOCKET BROADCAST ---
        // Push the new winning bid out to everyone currently staring at the auction room!
        log.debug("Broadcasting winning bid to WebSocket topic /topic/auctions/{}", auctionId);
        messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, response);
        
        return response;
    }
}
