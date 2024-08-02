## Spring boot RSocket Client
* HTTP 요청 -> R소켓 서버 연결 / 요청 / 응답 -> 클라이언트 전달

### 개발환경
- spring boot 2.7.17
- java 1.8
- webFlux
- Mongodb
- docker

### 실행
----
* 사전 실행 : 도커의 몽고디비 cmd  : docker run -p 27017-27019:27017-27019 mongo
* 사전 실행 : R소켓 서버 실행
----
1. 서버 실행
2. 모니터링 터미널  : curl -v localost:8080/items
3. 요청 터미널 : curl localost:8080/items/req-stream -H "Accept:application/x-ndjson"
4. 모니터링 데이터 확인
