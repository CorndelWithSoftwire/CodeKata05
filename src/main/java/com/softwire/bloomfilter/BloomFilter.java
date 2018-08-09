package com.softwire.bloomfilter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;

public class BloomFilter {

    private final int hashLength;
    private final int hashCount;
    private final boolean[] bitmap;

    public BloomFilter(int hashLength, int hashCount) {
        this.hashLength = hashLength;
        this.hashCount = hashCount;
        this.bitmap = new boolean[(int)Math.pow(2, hashLength)];
    }

    public void addWord(String word) {
        hashes(word).forEach(hash -> bitmap[hash] = true);
    }

    public boolean test(String word) {
        return hashes(word).allMatch(hash -> bitmap[hash]);
    }

    public double density() {
        int count = 0;
        for (boolean b : bitmap) {
            if (b) count++;
        }
        return ((double) count) / bitmap.length;
    }

    private IntStream hashes(String word) {
        try {
            // Calculate a full MD5 hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            ByteBuffer fullHash = ByteBuffer.wrap(digest.digest(word.getBytes(StandardCharsets.UTF_8)));

            return IntStream.range(0, hashCount).map(i -> {
                // Take bytes until we have enough to use as our hash
                int hash = 0;
                int length = 0;
                while (length < hashLength) {
                    hash = (hash << 8) | (fullHash.get() & 0xFF);
                    length += 8;
                }
                return hash >>> (length - hashLength);
            });
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
