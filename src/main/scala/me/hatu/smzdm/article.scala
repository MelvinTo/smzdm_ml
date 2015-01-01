package me.hatu.smzdm

import grizzled.slf4j.Logging

class Article(titlec : String, contentc: String = "") extends Logging {
	var title: String = titlec
	var link: String = ""
	var content: String = contentc
	var categories: List[String] = List[String]()
	var keywords: Map[String, Int] = Map[String, Int]()
	var valid: Boolean = true
	var article_id: String = ""

	override def toString : String = {
		return "[%s] %s %s" format (	categories.reverse.take(2).reverse.mkString(","), 
										title,
										link)
												// content.substring(0,25), 
												// keywords.toList.sortBy{x => x._2}.reverse.take(3).mkString("\n"))
	}

	def addCategory(category : String) {
		categories = categories :+ category
	}
}