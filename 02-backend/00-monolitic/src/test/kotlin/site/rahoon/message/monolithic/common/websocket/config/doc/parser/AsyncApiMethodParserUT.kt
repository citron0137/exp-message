package site.rahoon.message.monolithic.common.websocket.config.doc.parser

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.config.doc.AsyncApiFullGenerator
import java.time.LocalDateTime

class AsyncApiMethodParserUT {

    private val basePackage: String = "site.rahoon.message.monolithic"
//    private val asyncApiMethodParser: AsyncApiMethodParser =
//        AsyncApiMethodParser(basePackage)
//    private val asyncApiDtoParser: AsyncApiDtoParser = AsyncApiDtoParser()
//    private val asyncApiDTOMapper: AsyncApiDtoMapper = AsyncApiDtoMapper()

    private val asyncApiFullGenerator = AsyncApiFullGenerator(ObjectMapper())


//    @Test
//    fun findAllStompMethods(){
//        val methods = asyncApiMethodParser.findAllStompMethods()
//    }
//
//    @Test
//    fun findAllDtos(){
//        val methods = asyncApiMethodParser.findAllStompMethods()
//        val dtos = asyncApiDtoParser.extractAllDtos(methods)
//    }
//
//    @Test
//    fun mapDto(){
//        val methods = asyncApiMethodParser.findAllStompMethods()
//        val dtos = asyncApiDtoParser.extractAllDtos(methods)
//        val schemas = asyncApiDTOMapper.mapToSchemas(dtos)
//        println("=================================")
//        methods.forEach { println(it.declaringClass.canonicalName +":"+it.name) }
//        println("=================================")
//        dtos.forEach { println(it.canonicalName) }
//        println("=================================")
//        schemas.forEach { println(it.toString()) }
//        println("=================================")
//    }

    @Test
    fun parse(){
//        val methods = .findAllStompMethods()
//        val metadata = asyncApiMethodParser.parseToMetadata(methods)
//        metadata.forEach { println(it.toString()) }
        val g = asyncApiFullGenerator.generate(basePackage = basePackage)
        println(g.toString())
    }

}

