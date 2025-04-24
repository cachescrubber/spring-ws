/*
 * Copyright 2005-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.transport.mail.support;

import java.net.URI;

import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailTransportUtilsTests {

	@Test
	void testToPasswordProtectedString() {

		URLName name = new URLName("imap://john:secret@imap.example.com/INBOX");
		String result = MailTransportUtils.toPasswordProtectedString(name);

		assertThat(result.indexOf("secret")).isEqualTo(-1);
	}

	@Test
	void testGetTo() throws Exception {

		URI uri = new URI("mailto:infobot@example.com?subject=current-issue");
		InternetAddress to = MailTransportUtils.getTo(uri);

		assertThat(to).isEqualTo(new InternetAddress("infobot@example.com"));

		uri = new URI("mailto:infobot@example.com");
		to = MailTransportUtils.getTo(uri);

		assertThat(to).isEqualTo(new InternetAddress("infobot@example.com"));
	}

	@Test
	void testGetSubject() throws Exception {

		URI uri = new URI("mailto:infobot@example.com?subject=current-issue");
		String subject = MailTransportUtils.getSubject(uri);

		assertThat(subject).isEqualTo("current-issue");

		uri = new URI("mailto:infobot@example.com");
		subject = MailTransportUtils.getSubject(uri);

		assertThat(subject).isNull();
	}

}
