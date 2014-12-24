package me.hatu.smzdm

import org.scalatest.FunSuite
import grizzled.slf4j.Logging


class TestWebParser extends FunSuite with Logging {
	test("Should be able to handle non-existing link") {
		val article = WebParser.parse_article("title", "http://haitao.smzdm.com/youhui/311597")
		assert(article.valid == false)

		val article2 = WebParser.parse_article("title2", "http://www.thisurldoesnotexist.com")
		assert(article2.valid == false)
	}
}