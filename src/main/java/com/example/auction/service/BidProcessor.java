package com.example.auction.service;

import com.example.auction.model.AuctionItem;
import com.example.auction.model.BidHistory;
import com.example.auction.repository.AuctionItemRepository;
import com.example.auction.repository.BidHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BidProcessor {

    private final StringRedisTemplate redisTemplate;
    private final AuctionItemRepository auctionItemRepository;
    private final BidHistoryRepository bidHistoryRepository;

    public BidProcessor(StringRedisTemplate redisTemplate,
                        AuctionItemRepository auctionItemRepository,
                        BidHistoryRepository bidHistoryRepository) {
        this.redisTemplate = redisTemplate;
        this.auctionItemRepository = auctionItemRepository;
        this.bidHistoryRepository = bidHistoryRepository;
    }

    /**
     * Periodically process bids from the Redis queue and persist them to PostgreSQL.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processBids() {
        String queueKey = "auction:bids:queue";
        
        while (true) {
            String bidData = redisTemplate.opsForList().rightPop(queueKey);
            if (bidData == null) {
                break;
            }

            // Format: auctionId:userId:amount:timestamp
            String[] parts = bidData.split(":");
            if (parts.length == 4) {
                try {
                    Long auctionId = Long.parseLong(parts[0]);
                    Long userId = Long.parseLong(parts[1]);
                    BigDecimal amount = new BigDecimal(parts[2]);
                    LocalDateTime timestamp = LocalDateTime.parse(parts[3]);

                    AuctionItem item = auctionItemRepository.findById(auctionId).orElse(null);
                    if (item != null) {
                        // We only save if the incoming bid is actually higher than DB state
                        BigDecimal currentHighest = item.getCurrentHighestBid() != null ? item.getCurrentHighestBid() : item.getStartingPrice();
                        if (amount.compareTo(currentHighest) > 0) {
                            item.setCurrentHighestBid(amount);
                            item.setHighestBidderId(userId);
                            auctionItemRepository.save(item);

                            BidHistory history = new BidHistory();
                            history.setAuctionItemId(auctionId);
                            history.setUserId(userId);
                            history.setBidAmount(amount);
                            history.setTimestamp(timestamp);
                            bidHistoryRepository.save(history);
                            
                            log.debug("Persisted bid of {} for auction {} to PostgreSQL from Redis queue", amount, auctionId);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process bid from queue: {}", bidData, e);
                }
            }
        }
    }
}
