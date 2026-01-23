// package site.rahoon.message.monolithic.common.websocket.config.doc

// import org.springframework.messaging.handler.annotation.SendTo
// import org.springframework.stereotype.Component
// import java.time.LocalDateTime

// class WebSocketDocTest {

//     data class GenericClass<T> (
//         val data: T
//     )

//     data class TestSampleClass(
//         val number: Int,
//         val string: String,
//         val time: LocalDateTime,
//         val optional: String?,
//         val innerClass: InnerClass
//     ){
//         data class InnerClass(
//             val number: Int,
//             val string: String,
//             val time: LocalDateTime,
//             val optional: String?,
//         )
//     }

//     @Component
//     class TestWebSocketClass(){

//         @SendTo("/topic/test")
//         fun testClass(): TestSampleClass {
//             TODO()
//         }

//         @SendTo("/topic/test-list")
//         fun testList(): List<TestSampleClass>{
//             TODO()
//         }

//         @SendTo("/topic/test-generic")
//         fun testGeneric(): GenericClass<TestSampleClass>{
//             TODO()
//         }
//     }
// }
