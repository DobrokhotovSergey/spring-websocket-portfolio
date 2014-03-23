/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.portfolio.web.load;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * A ChannelInterceptor that caches mesages.
 */
public class TestMessageHandler implements MessageHandler {

	private final BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<>();

	private final List<String> destinationPatterns = new ArrayList<>();

	private final PathMatcher matcher = new AntPathMatcher();

	private volatile boolean isRecording;


	/**
	 * @param autoStart whether to start recording messages removing the need to
	 * 	call {@link #startRecording()} explicitly
	 */
	public TestMessageHandler(boolean autoStart) {
		this.isRecording = autoStart;
	}

	public void setIncludedDestinations(String... patterns) {
		this.destinationPatterns.addAll(Arrays.asList(patterns));
	}

	public void startRecording() {
		this.isRecording = true;
	}

	public void stopRecording() {
		this.isRecording = false;
	}

	public Message<?> awaitMessage(long timeoutInMillis) throws InterruptedException {
		return this.messages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		if (this.isRecording) {
			if (this.destinationPatterns.isEmpty()) {
				this.messages.add(message);
			}
			else {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (headers.getDestination() != null) {
					for (String pattern : this.destinationPatterns) {
						if (this.matcher.match(pattern, headers.getDestination())) {
							this.messages.add(message);
							break;
						}
					}
				}
			}
		}
	}

}
