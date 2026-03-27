package com.example.auction.service;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.model.BidHistory;
import com.example.auction.model.User;
import com.example.auction.repository.AuctionItemRepository;
import com.example.auction.repository.BidHistoryRepository;
import com.example.auction.repository.UserRepository;
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

    public AuctionService(AuctionItemRepository auctionItemRepository,
                          BidHistoryRepository bidHistoryRepository,
                          UserRepository userRepository) {
        this.auctionItemRepository = auctionItemRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.userRepository = userRepository;
    }

    public List<AuctionItem> getAllAuctions() {
        return auctionItemRepository.findAll();
    }

    public AuctionItem getAuctionById(Long id) {
        return auctionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction item not found"));
    }

    /**
     * THE CORE ENGINE — This is where Optimistic Locking happens.
     *
     * How it works (mapping to your Node.js knowledge):
     * ─────────────────────────────────────────────────────
     * In Express, you would manually do something like:
     *   1. const item = await db.query("SELECT * FROM items WHERE id = $1", [id]);
     *   2. if (newBid > item.currentHighestBid) { await db.query("UPDATE ..."); }
     *
     * The problem? Between step 1 and step 2, another user could have already
     * updated the row. You'd overwrite their bid. That's a RACE CONDITION.
     *
     * Spring Boot + @Version solves this automatically:
     *   1. We fetch the item (Hibernate remembers the current `version` number).
     *   2. We modify the item in Java memory.
     *   3. When Hibernate flushes the save, it generates:
     *      UPDATE auction_items SET ... WHERE id = ? AND version = ?
     *      If the version in the DB has changed (someone else saved first),
     *      the UPDATE affects 0 rows → Hibernate throws
     *      ObjectOptimisticLockingFailureException.
     *   4. We catch that exception and tell the user "try again, you were outbid."
     */
    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest bidRequest, String username) {

        // 1. Fetch the auction item (Hibernate snapshots the current @Version)
        AuctionItem item = auctionItemRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction item not found"));

        // 2. Business validation: Is the auction still active?
        if (item.getEndTime().isBefore(LocalDateTime.now())) {
            return new BidResponse("REJECTED", "Auction has ended", auctionId, 
                    item.getCurrentHighestBid(), null);
        }

        // 3. Business validation: Is the bid actually higher?
        BigDecimal minimumBid = item.getCurrentHighestBid() != null 
                ? item.getCurrentHighestBid() 
                : item.getStartingPrice();

        if (bidRequest.getAmount().compareTo(minimumBid) <= 0) {
            return new BidResponse("REJECTED", 
                    "Bid must be higher than current highest bid: " + minimumBid, 
                    auctionId, minimumBid, null);
        }

        // 4. Look up the bidder
        User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 5. Update the item (still in Java memory — not saved yet)
        item.setCurrentHighestBid(bidRequest.getAmount());
        item.setHighestBidderId(bidder.getId());

        // 6. Try to save — THIS is where the @Version check fires!
        //    If another thread already incremented the version, Hibernate will
        //    throw ObjectOptimisticLockingFailureException here.
        try {
            auctionItemRepository.save(item);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Another user's bid was committed a split-second before ours.
            // Re-fetch to get the current state and tell our user what happened.
            AuctionItem freshItem = auctionItemRepository.findById(auctionId).orElse(item);
            return new BidResponse("REJECTED", 
                    "Someone placed a higher bid just before you! Try again.", 
                    auctionId, freshItem.getCurrentHighestBid(), null);
        }

        // 7. Record this bid in the history log
        BidHistory history = new BidHistory();
        history.setAuctionItemId(auctionId);
        history.setUserId(bidder.getId());
        history.setBidAmount(bidRequest.getAmount());
        history.setTimestamp(LocalDateTime.now());
        bidHistoryRepository.save(history);

        // 8. Return success
        return new BidResponse("ACCEPTED", "Your bid has been placed!", 
                auctionId, bidRequest.getAmount(), username);
    }
}
