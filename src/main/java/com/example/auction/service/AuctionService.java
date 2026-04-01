package com.example.auction.service;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.model.BidHistory;
import com.example.auction.model.User;
import com.example.auction.repository.AuctionItemRepository;
import com.example.auction.repository.BidHistoryRepository;
import com.example.auction.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
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

    public List<AuctionItem> getAllAuctions() {
        return auctionItemRepository.findAll();
    }

    public AuctionItem getAuctionById(Long id) {
        return auctionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction item not found"));
    }

    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, String username) {

        AuctionItem item = auctionItemRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction item not found"));

        if (item.getEndTime().isBefore(LocalDateTime.now())) {
            return new BidResponse("REJECTED", "Auction has ended", auctionId, 
                    item.getCurrentHighestBid(), null);
        }

        BigDecimal minimumBid = item.getCurrentHighestBid() != null 
                ? item.getCurrentHighestBid() 
                : item.getStartingPrice();

        if (bidRequest.getAmount().compareTo(minimumBid) <= 0) {
            return new BidResponse("REJECTED", 
                    "Bid must be higher than current highest bid: " + minimumBid, 
                    auctionId, minimumBid, null);
        }

        User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        item.setCurrentHighestBid(bidRequest.getAmount());
        item.setHighestBidderId(bidder.getId());

        try {
            auctionItemRepository.save(item);
        } catch (ObjectOptimisticLockingFailureException e) {
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

        BidResponse response = new BidResponse("ACCEPTED", "Your bid has been placed!", 
                auctionId, bidRequest.getAmount(), username);

        // --- THE WEBSOCKET BROADCAST ---
        // Push the new winning bid out to everyone currently staring at the auction room!
        messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, response);
        
        return response;
    }
}
