package io.dennis.inventory.demo.service;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.UpdateInventoryRequest;
import io.dennis.inventory.demo.entity.Inventory;
import io.dennis.inventory.demo.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts ten real transactions against the same row and version.
 * This test is intentionally not @Transactional so each worker runs in its own transaction.
 * A separate H2 database isolates its committed data from other tests.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory-concurrency;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000"
})
class InventoryConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void onlyOneOfTenConcurrentUpdatesSucceeds() throws Exception {
        String inventoryName = "Concurrent Cake " + UUID.randomUUID();
        InventoryView created = inventoryService.create(
                new CreateInventoryRequest(inventoryName, 1, 3, 10));

        try {
            Queue<InventoryView> successes = new ConcurrentLinkedQueue<>();
            Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(THREAD_COUNT);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

            try {
                for (int i = 1; i <= THREAD_COUNT; i++) {
                    int updatedQuantity = i;
                    executor.submit(() -> {
                        try {
                            startGate.await();
                            successes.add(inventoryService.update(new UpdateInventoryRequest(
                                    created.id(), created.version(), updatedQuantity)));
                        } catch (Throwable failure) {
                            failures.add(failure);
                        } finally {
                            done.countDown();
                        }
                    });
                }

                startGate.countDown();
                assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }

            assertThat(successes).hasSize(1);
            assertThat(failures)
                    .hasSize(THREAD_COUNT - 1)
                    .allSatisfy(failure -> assertThat(isVersionConflict(failure)).isTrue());

            InventoryView winner = successes.peek();
            Inventory stored = inventoryRepository.findById(created.id()).orElseThrow();
            assertThat(stored.getVersion()).isEqualTo(created.version() + 1);
            assertThat(stored.getQuantity()).isEqualTo(winner.quantity());
        } finally {
            inventoryRepository.deleteById(created.id());
        }
    }

    private boolean isVersionConflict(Throwable failure) {
        if (failure instanceof BusinessException businessException) {
            return businessException.getErrorCode() == ErrorCode.VERSION_CONFLICT;
        }
        return failure instanceof ObjectOptimisticLockingFailureException;
    }
}
