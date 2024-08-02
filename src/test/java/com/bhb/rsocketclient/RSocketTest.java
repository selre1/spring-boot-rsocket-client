package com.bhb.rsocketclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
public class RSocketTest {

    @Autowired
    WebTestClient webTestClient;
    @Autowired ItemRepository itemRepository;


    @Test
    @DisplayName("Http요청 ->  R소켓 연결 및 전달응답 -> 클라이언트에데 리액티브하게 반환")
    void verifyRemoteOperations() throws InterruptedException {
        itemRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        webTestClient.post().uri("/items/req-res")
                .bodyValue(new Item("아이디","방항배","수지구",123.22))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Item.class)
                .value(item -> {
                    assertThat(item.getId()).isEqualTo("아이디");
                    assertThat(item.getName()).isEqualTo("방항배");
                    assertThat(item.getPrice()).isEqualTo(123.22);
                });

        Thread.sleep(1000);

        itemRepository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("방항배");
                    assertThat(item.getPrice()).isEqualTo(123.22);
                    return true;
                }).verifyComplete();
    }

    @Test
    @DisplayName("Request - Stream")
    void verifyRemoteOperationsReqStream(){
        itemRepository.deleteAll().as(StepVerifier::create).verifyComplete();

       List<Item> items = IntStream.rangeClosed(1,3)
                        .mapToObj(value -> new Item("name-"+value,"방","항",12.22))
                                .collect(Collectors.toList());

       itemRepository.saveAll(items).blockLast();

        webTestClient.get().uri("/items/req-stream")
                .accept(MediaType.APPLICATION_NDJSON) // json 스트림으로 받겠다고 R소켓 클라이언트에게 알림
                .exchange()
                .expectStatus().isOk()
                .returnResult(Item.class)
                .getResponseBody()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("방");
                    assertThat(item.getPrice()).isEqualTo(12.22);
                    return true;
                })
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("방");
                    assertThat(item.getPrice()).isEqualTo(12.22);
                    return true;
                })
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("방");
                    assertThat(item.getPrice()).isEqualTo(12.22);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("R소켓 실행 후 망각 void")
    public void verifyRemoteOperationsFireAanForget() throws InterruptedException {
        itemRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        webTestClient.post().uri("/items/fire-and-forget")
                .bodyValue(new Item("아이디","방항배","수지구",123.22))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
         Thread.sleep(500);

        itemRepository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("방항배");
                    assertThat(item.getPrice()).isEqualTo(123.22);
                    return true;
                }).verifyComplete();
    }
}
