package me.hatu.smzdm

import org.scalatest.FunSuite

class TestArticle extends FunSuite {
	test("Article should have title") {
		val article = new Article("titleA")
		assert(article.title == "titleA")
	}
}