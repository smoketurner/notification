/**
 * Copyright 2016 Smoke Turner, LLC.
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
package com.smoketurner.notification.application.core;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import io.dropwizard.util.Duration;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Rule {

    private final Integer maxSize;
    private final Duration maxDuration;
    private final String matchOn;

    /**
     * Constructor
     *
     * @param maxSize
     *            Maximum number of notifications to include in a roll-up
     * @param maxDuration
     *            Maximum time duration between the first and last notifications
     *            in a roll-up
     * @param matchOn
     *            Group notifications by a specific category
     */
    @JsonCreator
    public Rule(@JsonProperty("max-size") final Optional<Integer> maxSize,
            @JsonProperty("max-duration") final Optional<Duration> maxDuration,
            @JsonProperty("match-on") final Optional<String> matchOn) {
        this.maxSize = maxSize.orNull();
        this.maxDuration = maxDuration.orNull();
        this.matchOn = matchOn.orNull();
    }

    @JsonProperty("max-size")
    public Optional<Integer> getMaxSize() {
        return Optional.fromNullable(maxSize);
    }

    @JsonProperty("max-duration")
    public Optional<Duration> getMaxDuration() {
        return Optional.fromNullable(maxDuration);
    }

    @JsonProperty("match-on")
    public Optional<String> getMatchOn() {
        return Optional.fromNullable(matchOn);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final Rule other = (Rule) obj;
        return Objects.equals(maxSize, other.maxSize)
                && Objects.equals(maxDuration, other.maxDuration)
                && Objects.equals(matchOn, other.matchOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSize, maxDuration, matchOn);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("maxSize", maxSize)
                .add("maxDuration", maxDuration).add("matchOn", matchOn)
                .toString();
    }
}
