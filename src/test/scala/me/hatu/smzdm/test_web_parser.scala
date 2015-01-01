package me.hatu.smzdm

import org.scalatest.FunSuite
import grizzled.slf4j.Logging


class TestWebParser extends FunSuite with Logging {
	test("Should be able to handle non-existing link") {
    val article1 = new Article("title1")
    article1.link = "http://haitao.smzdm.com/youhui/311597fff"
    val article2 = new Article("title2")
    article2.link = "http://haitao.smzdm.com/youhui/3115fff97fff"

    val enriched1 = WebParser.enrichArticle(article1)
    val enriched2 = WebParser.enrichArticle(article2)

		assert(enriched1.valid == false)
		assert(enriched2.valid == false)
	}
}