package com.example.auction.component;

import com.example.auction.model.AuctionItem;
import com.example.auction.repository.AuctionItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AuctionItemRepository auctionItemRepository;

    public DataSeeder(AuctionItemRepository auctionItemRepository) {
        this.auctionItemRepository = auctionItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (auctionItemRepository.count() == 0) {
            AuctionItem item1 = new AuctionItem();
            item1.setName("Vintage Rolex Submariner");
            item1.setDescription("A beautiful 1980s Rolex Submariner in excellent condition.");
            item1.setStartingPrice(new BigDecimal("5000.00"));
            item1.setCurrentHighestBid(null);
            item1.setHighestBidderId(null);
            item1.setEndTime(LocalDateTime.now().plusDays(365));
            // version defaults to 0 upon save
            auctionItemRepository.save(item1);

            AuctionItem item2 = new AuctionItem();
            item2.setName("First Edition Charizard - PSA 10");
            item2.setDescription("The holy grail of Pokémon trading cards.");
            item2.setStartingPrice(new BigDecimal("150000.00"));
            item2.setCurrentHighestBid(null);
            item2.setHighestBidderId(null);
            item2.setEndTime(LocalDateTime.now().plusDays(365));
            auctionItemRepository.save(item2);

            System.out.println("Seeded database with two active test Auction Items!");
        }
    }
}
