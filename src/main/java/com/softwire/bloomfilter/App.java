package com.softwire.bloomfilter;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class App {

    private static final Random RANDOM = new Random();
    private static final int TEST_CASES = 1000;

    private static Set<String> words = fetchWords();

    public static void main(String[] args) {
        System.out.println(" Hash Length | Hash Count | Density    | False Positives");
        System.out.println("---------------------------------------------------------");
        for (int hashLength = 16; hashLength < 32; hashLength += 2) {
            for (int hashCount = 1; hashCount <= 128 / (((hashLength + 7) / 8) * 8); hashCount += 1) {
                BloomFilter filter = new BloomFilter(hashLength, hashCount);
                words.forEach(filter::addWord);
                double falsePositives = testFilter(filter);

                System.out.println(String.format(" %-11d | %-10d | %.8f | %.8f",
                        hashLength,
                        hashCount,
                        filter.density(),
                        falsePositives));
            }
        }
    }

    private static double testFilter(BloomFilter filter) {
        int falsePositives = 0;

        for (int i = 0; i < TEST_CASES; i++) {
            String str = randomString();
            if (filter.test(str)) {
                if (!words.contains(str)) {
                    falsePositives++;
                }
            } else if (words.contains(str)) {
                throw new RuntimeException("Filter missed a valid match!");
            }
        }

        return ((double) falsePositives) / TEST_CASES;
    }

    private static String randomString() {
        return RANDOM.ints(5, 'a', 'z' + 1)
                .mapToObj(c -> Character.toString((char) c))
                .collect(Collectors.joining());
    }

    private static Set<String> fetchWords() {
        try {
            HttpClient client = HttpClients.createDefault();
            HttpEntity entity = client.execute(new HttpGet("http://codekata.com/data/wordlist.txt")).getEntity();
            String response = EntityUtils.toString(entity);
            return Arrays.stream(response.split("\n")).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
