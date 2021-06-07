/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.externalreport.androidlint

import java.io.IOException
import java.io.InputStream
import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.StartElement
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory

private val ISSUES_ELEMENT = QName("issues")
private val ISSUE_ELEMENT = QName("issue")
private val ID_ATTRIBUTE = QName("id")
private val MESSAGE_ATTRIBUTE = QName("message")
private val LOCATION_ELEMENT = QName("location")
private val FILE_ATTRIBUTE = QName("file")
private val LINE_ATTRIBUTE = QName("line")

internal class AndroidLintXmlReportReader private constructor(private val consumer: IssueConsumer) {
    private var level = 0
    private var id = ""
    private var message = ""
    private var file = ""
    private var line = ""

    internal fun interface IssueConsumer {
        fun onIssue(id: String, file: String, line: String, message: String)
    }

    @Throws(XMLStreamException::class, IOException::class)
    private fun read(inputStream: InputStream) {
        val reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(inputStream)
        while (reader.hasNext()) {
            val event = reader.nextEvent()
            if (event.isStartElement) {
                level++
                onElement(event.asStartElement())
            } else if (event.isEndElement) {
                level--
                onEndElement()
            }
        }
    }

    private fun onEndElement() {
        if (level == 1) {
            consumer.onIssue(id, file, line, message)
            id = ""
            message = ""
            file = ""
            line = ""
        }
    }

    private fun onElement(element: StartElement) {
        if (level == 1 && ISSUES_ELEMENT != element.name) {
            throw IOException("Unexpected document root '" + element.name.localPart + "' instead of 'issues'.")
        } else if (level == 2 && ISSUE_ELEMENT == element.name) {
            id = getAttributeValue(element, ID_ATTRIBUTE)
            message = getAttributeValue(element, MESSAGE_ATTRIBUTE)
        } else if (level == 3 && LOCATION_ELEMENT == element.name && file.isEmpty()) {
            file = getAttributeValue(element, FILE_ATTRIBUTE)
            line = getAttributeValue(element, LINE_ATTRIBUTE)
        }
    }

    companion object {
        @Throws(XMLStreamException::class, IOException::class)
        fun read(inputStream: InputStream, consumer: IssueConsumer) {
            AndroidLintXmlReportReader(consumer).read(inputStream)
        }

        private fun getAttributeValue(element: StartElement, attributeName: QName): String {
            val attribute = element.getAttributeByName(attributeName)
            return if (attribute != null) attribute.value else ""
        }
    }
}
