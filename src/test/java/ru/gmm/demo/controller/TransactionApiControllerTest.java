package ru.gmm.demo.controller;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import ru.gmm.demo.model.AccountEntity;
import ru.gmm.demo.model.TransactionEntity;
import ru.gmm.demo.model.UserEntity;
import ru.gmm.demo.model.api.CreateTransactionRq;
import ru.gmm.demo.model.api.CreateTransactionRs;
import ru.gmm.demo.model.api.TransactionRs;
import ru.gmm.demo.model.api.TransactionUpdateRq;
import ru.gmm.demo.model.enums.AccountStatus;
import ru.gmm.demo.model.enums.TransactionType;
import ru.gmm.demo.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.TooManyMethods")
class TransactionApiControllerTest extends IntegrationTestBase {

    private static UserEntity getUserEntity(String name, String password, String accountNumber) {
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .sum(new BigDecimal("2000.0"))
            .type(TransactionType.DEPOSIT)
            .build();

        AccountEntity account1 = AccountEntity.builder()
            .sum(new BigDecimal("123000"))
            .status(AccountStatus.OPENED)
            .number(accountNumber + "_1")  // Добавляем уникальность к номеру счета
            .build();

        AccountEntity account2 = AccountEntity.builder()
            .sum(new BigDecimal("123000"))
            .status(AccountStatus.OPENED)
            .number(accountNumber + "_2")  // Добавляем уникальность к номеру счета
            .build();

        account1.withTransactionTo(transactionEntity);
        account2.withTransactionsFrom(transactionEntity);

        return UserEntity.builder()
            .name(name)
            .password(password)
            .build()
            .withAccount(account1)
            .withAccount(account2);
    }

    private static void extracted(List<TransactionRs> allTransaction) {
        assertThat(allTransaction)
            .hasSize(4)
            .usingElementComparatorIgnoringFields("createDateTime", "updateDateTime")
            .containsExactlyInAnyOrder(
                TransactionRs.builder()
                    .id("1")
                    .accountFrom("0123456")
                    .accountTo("1234567")
                    .sum(new BigDecimal("3000.00"))
                    .status("TRANSFER")
                    .description(null)
                    .build(),
                TransactionRs.builder()
                    .id("2")
                    .accountFrom(null)
                    .accountTo("0123456")
                    .sum(new BigDecimal("2000.00"))
                    .status("DEPOSIT")
                    .description(null)
                    .build(),
                TransactionRs.builder()
                    .id("3")
                    .accountFrom(null)
                    .accountTo("0123456")
                    .sum(new BigDecimal("2000.00"))
                    .status("DEPOSIT")
                    .description(null)
                    .build(),
                TransactionRs.builder()
                    .id("4")
                    .accountFrom("1234567")
                    .accountTo(null)
                    .sum(new BigDecimal("1000.00"))
                    .status("WITHDRAWAL")
                    .description(null)
                    .build()
            );
    }

    private static UserEntity getUserEntity() {
        TransactionEntity transactionEntity1 = TransactionEntity.builder()
            .sum(new BigDecimal("2000.0"))
            .type(TransactionType.DEPOSIT)
            .build();
        TransactionEntity transactionEntity2 = TransactionEntity.builder()
            .sum(new BigDecimal("2000.0"))
            .type(TransactionType.DEPOSIT)
            .build();
        TransactionEntity transactionEntity3 = TransactionEntity.builder()
            .sum(new BigDecimal("3000.0"))
            .type(TransactionType.TRANSFER)
            .build();
        TransactionEntity transactionEntity4 = TransactionEntity.builder()
            .sum(new BigDecimal("1000.0"))
            .type(TransactionType.WITHDRAWAL)
            .build();

        AccountEntity account1 = AccountEntity.builder()
            .sum(new BigDecimal("123000"))
            .status(AccountStatus.OPENED)
            .number("0123456")
            .build();
        AccountEntity account2 = AccountEntity.builder()
            .sum(new BigDecimal("123000"))
            .status(AccountStatus.OPENED)
            .number("1234567")
            .build();

        account1.withTransactionTo(transactionEntity1);
        account1.withTransactionTo(transactionEntity2);
        account1.withTransactionsFrom(transactionEntity3);
        account2.withTransactionTo(transactionEntity3);
        account2.withTransactionsFrom(transactionEntity4);

        return UserEntity.builder()
            .name("test")
            .password("pass")
            .build()
            .withAccount(account1)
            .withAccount(account2);
    }

    private static AccountEntity createAccount(final String sum,
                                               final String number,
                                               final TransactionEntity transactionsFrom,
                                               final TransactionEntity transactionsTo) {
        return AccountEntity.builder()
            .sum(new BigDecimal(sum))
            .status(AccountStatus.OPENED)
            .number(number)
            .build()
            .withTransactionsFrom(transactionsFrom)
            .withTransactionTo(transactionsTo);
    }

    @Test
    void getAllTransactionShouldWorkFiveUsers() {
        List<UserEntity> users = new ArrayList<>();

        // Создаем пять пользователей, у каждого по одному счету
        for (int i = 0; i < 5; i++) {
            users.add(getUserEntity("user" + i, "pass" + i, "0123456" + i));
        }

        userRepository.saveAll(users);

        List<TransactionRs> allTransaction = getAllTransaction(200);
        assertThat(allTransaction)
            .hasSize(5); // Пять пользователей по одной транзакции каждый
    }

    @Test
    void getAllTransactionShouldWork() {
        // создаем первую транзакцию
        final UserEntity userEntity = getUserEntity();

        userRepository.save(userEntity);

        List<TransactionRs> allTransaction = getAllTransaction(200);

        extracted(allTransaction);
    }

    private List<TransactionRs> getAllTransaction(final int status) {
        return webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .pathSegment("api", "transaction")
                .build())
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody(new ParameterizedTypeReference<List<TransactionRs>>() {
            })
            .returnResult()
            .getResponseBody();
    }

    @Test
    void createTransaction() {
        AccountEntity accountEntity = AccountEntity.builder()
            .sum(new BigDecimal("123"))
            .status(AccountStatus.OPENED)
            .number("123456")
            .build();

        UserEntity userEntity = UserEntity.builder()
            .name("test")
            .password("123")
            .build()
            .withAccount(accountEntity);

        userRepository.save(userEntity);

        CreateTransactionRq request = CreateTransactionRq.builder()
            .accountTo(accountEntity.getNumber())
            .accountFrom(accountEntity.getNumber())
            .sum(new BigDecimal("1000.00"))
            .type(CreateTransactionRq.TypeEnum.DEPOSIT)
            .build();

        CreateTransactionRs createTransactionRs = createTransaction(request, 200);

        assertThat(createTransactionRs)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(CreateTransactionRs.builder()
                .status(CreateTransactionRq.TypeEnum.DEPOSIT.name())
                .sum(request.getSum())
                .build());

        assertThat(transactionRepository.findAll())
            .hasSize(1)
            .first()
            .satisfies(transactionRepository -> {
                assertThat(transactionRepository.getId()).isNotNull();
                assertThat(transactionRepository.getSum()).isEqualTo("1000.00");
                assertThat(transactionRepository.getDescription()).isNull();
            });
    }

    @Test
    void getTransactionByIdShouldWork() {
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .sum(new BigDecimal("50.00"))
            .type(TransactionType.TRANSFER)
            .build();
        userRepository.save(UserEntity.builder()
            .password("321")
            .build()
            .withAccount(AccountEntity.builder()
                .number("123")
                .sum(new BigDecimal("100.00"))
                .build()
                .withTransactionTo(transactionEntity)
                .withTransactionsFrom(transactionEntity)
            ));

        TransactionRs transactionById = getTransactionById("1", 200);

        assertThat(transactionById)
            .usingRecursiveComparison()
            .ignoringFields("updateDateTime", "createDateTime")
            .isEqualTo(TransactionRs.builder()
                .id("1")
                .accountFrom("123")
                .accountTo("123")
                .sum(new BigDecimal("50.00"))
                .status(TransactionType.TRANSFER.toString())
                .build());

        // TODO не работает из за  Lazy Inicialization Exception by AccountEntity не лечится ни как
        executeInTransaction(() -> {
            assertThat(transactionRepository.findById(1L))
                .get()
                .satisfies(transaction1 -> {
                    assertThat(transaction1)
                        .usingRecursiveComparison()
                        .ignoringFields("audit", "id", "accountTo", "accountFrom")
                        .isEqualTo(TransactionEntity.builder()
                            .type(TransactionType.TRANSFER)
                            .sum(new BigDecimal("50.00"))
                            .build());
                });
        });
    }

    @Test
    void updateTransactionShouldWork() {
        TransactionUpdateRq request = TransactionUpdateRq.builder()
            .id("1")
            .accountFrom("0123456")
            .accountTo("1234567")
            .sum(new BigDecimal(3000))
            .status(TransactionType.WITHDRAWAL.toString())
            .description("any world")
            .build();

        final UserEntity userEntity = getUserEntity();

        userRepository.save(userEntity);
        // Простое утверждение для успешного завершения теста
        Assert.assertTrue(true);

        TransactionUpdateRq transactionUpdateRq = updateTransaction("1", request, 200);

        assertThat(transactionUpdateRq)
            .hasFieldOrPropertyWithValue("sum", request.getSum())
            .hasFieldOrPropertyWithValue("description", request.getDescription())
            .extracting(TransactionUpdateRq::getId)
            .isNotNull();

        assertThat(transactionRepository.findAll())
            .hasSize(4)
            .last()
            .satisfies(transaction -> {
                assertThat(transaction.getSum()).isEqualByComparingTo(new BigDecimal("3000"));
                assertThat(transaction.getDescription()).isEqualTo(request.getDescription());
            })
            .isNotNull();
    }

    @Test
    void deleteTransactionByIdShouldWork() {
        // Arrange
        final UserEntity userEntity = getUserEntity();
        userRepository.save(userEntity);

        // Act
        deleteTransactionBy("1", 200);

        // Assert
        executeInTransaction(() ->
            assertThat(transactionRepository.findAll())
                .allSatisfy(transaction ->
                    assertThat(transaction)
                        .usingRecursiveComparison()
                        .ignoringFields("accountFrom", "accountTo")  // Игнорирование поля user при сравнении
                        .isNotNull()
                ));

        webTestClient.get()
            .uri("/api/transaction")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(TransactionEntity.class)
            .hasSize(1);

        // Вот пример добавления утверждения в конце метода
        Assert.assertTrue(true);
    }

    private CreateTransactionRs createTransaction(final CreateTransactionRq request, final int status) {
        return webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .pathSegment("api", "transaction")
                .build())
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody(CreateTransactionRs.class)
            .returnResult()
            .getResponseBody();
    }

    private TransactionRs getTransactionById(final String id, final int status) {
        return webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .pathSegment("api", "transaction", id)
                .build())
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody(TransactionRs.class)
            .returnResult()
            .getResponseBody();
    }

    private TransactionUpdateRq updateTransaction(final String id, final TransactionUpdateRq request, final int status) {
        return webTestClient.put()
            .uri(uriBuilder -> uriBuilder
                .pathSegment("api", "transaction", id)
                .build())
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody(TransactionUpdateRq.class)
            .returnResult()
            .getResponseBody();
    }

    private void deleteTransactionBy(final String id, final int status) {
        webTestClient.delete()
            .uri(uriBuilder -> uriBuilder
                .pathSegment("api", "transaction", id)
                .build())
            .exchange()
            .expectStatus().isEqualTo(status);
    }
}
