package com.bhb.rsocketclient;

import io.rsocket.metadata.WellKnownMimeType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@RestController
public class RSocketController {
    private final Mono<RSocketRequester> requesterMono;


    public RSocketController(RSocketRequester.Builder builder) {
        this.requesterMono = builder.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .metadataMimeType(MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.toString()))
                .connectTcp("localhost",7000)// R소켓 서버와 연결
                .retry(5)
                .cache(); //핫소스 전환 : 가장 최근 신호는 캐시 // 다수의 클라이언트가 동일한 하나의 데이터를 요구할떄 효율성 높임
    }

    @PostMapping("/items/req-res")
    public Mono<ResponseEntity<?>> addNewItemRsocketReqRes(@RequestBody Item item){
        return requesterMono
                .flatMap(rSocketRequester -> rSocketRequester
                    .route("newItems.req-res")
                    .data(item)
                    .retrieveMono(Item.class))
                .map(savedItem -> ResponseEntity.created(URI.create("/item/req-res")).body(savedItem));
    }


    /*
    * http 요청을 처리하고 flux를 통해 json 스트림으로 반환
    * flatMapMany -> 여러 건의 조회 결과 반환
    * delayElements -> 1초에 1건씩 반환  (응답을 눈으로 확인하기 위함)
    *
    * Flux<Item> -> 스트림으로 반환해야하기때문!!
    * */
    @GetMapping(value = "/items/req-stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Item> findItemsUsingRsocketReqStream(){
        return requesterMono.flatMapMany(
                rSocketRequester -> rSocketRequester
                        .route("newItems.req-stream")
                        .retrieveFlux(Item.class)
                        .delayElements(Duration.ofSeconds(1))
        );
    }

    @PostMapping("/items/fire-and-forget")
    public Mono<ResponseEntity<?>> addNewItemRsocketFireAndForget(@RequestBody Item item){
        return requesterMono
                .flatMap(rSocketRequester -> rSocketRequester
                        .route("newItems.fire-and-forget")
                        .data(item)
                        .send()) // ===>>>>> mono<void> 반환받기 위함
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping(value = "/items", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Item> liveUpdates(){
        //flatMapMany 여러건의 조회 결과를 Flux에 담아 반환하도록 (스트림을 쓰니까?)
         return requesterMono.flatMapMany(
                 rSocketRequester -> rSocketRequester
                         .route("newItems.monitor")
                         .retrieveFlux(Item.class)
         );
    }
}
