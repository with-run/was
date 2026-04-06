package kr.withrun.was.domain.reward.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RewardDrawRandomGenerator {

    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
