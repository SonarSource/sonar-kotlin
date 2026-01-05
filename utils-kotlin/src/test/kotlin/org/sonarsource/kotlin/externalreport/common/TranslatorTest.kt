/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.externalreport.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TranslatorTest {

    @Test
    fun `should escape html`() {

        val input = """
            <!DOCTYPE html>
            <html>
              <head>
                <title>Largest companies by market cap — US Stock Market</title>
                <meta charset="UTF-8" />
              </head>
              <body>
                <p>Apple : 2037 Billion</p>
                <p>Microsoft : 1624 Billion</p>
                <p>Amazon : 1611 Billion</p>
                <p>Google : 1058 Billion</p>
                <p>Alibaba : 826 Billion</p>

                This data is as of 21 Sep 2020.
                😂
              </body>
            </html>
        """.trimIndent()

        assertThat( Translator.escapeHtml4(input)).isEqualToIgnoringWhitespace("""
            &lt;!DOCTYPE html&gt;
            &lt;html&gt;
              &lt;head&gt;
                &lt;title&gt;Largest companies by market cap &mdash; US Stock Market&lt;/title&gt;
                &lt;meta charset=&quot;UTF-8&quot; /&gt;
              &lt;/head&gt;
              &lt;body&gt;
                &lt;p&gt;Apple : 2037 Billion&lt;/p&gt;
                &lt;p&gt;Microsoft : 1624 Billion&lt;/p&gt;
                &lt;p&gt;Amazon : 1611 Billion&lt;/p&gt;
                &lt;p&gt;Google : 1058 Billion&lt;/p&gt;
                &lt;p&gt;Alibaba : 826 Billion&lt;/p&gt;
            
                This data is as of 21 Sep 2020.
                😂
              &lt;/body&gt;
            &lt;/html&gt;""".trimIndent()
        )

    }

}
