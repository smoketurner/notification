package com.smoketurner.notification.application.core;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.smoketurner.notification.api.Notification;

public class Matcher implements Comparable<Matcher> {

  private final Notification notification;
  private final Rule rule;
  private final TreeSet<Notification> notifications = Sets.newTreeSet();
  private final Integer maxSize;
  private final Long firstMillis;
  private final Long maxDuration;
  private final String matchOn;
  private final String matchValue;

  /**
   * Constructor
   *
   * @param rule Rule for this match
   * @param notification First notification to match the rule
   */
  public Matcher(@Nonnull final Rule rule, @Nonnull final Notification notification) {
    this.rule = Preconditions.checkNotNull(rule);
    this.notification = Preconditions.checkNotNull(notification);

    this.matchOn = rule.getMatchOn().orNull();
    this.maxSize = rule.getMaxSize().orNull();
    if (matchOn != null) {
      this.matchValue = notification.getProperties().get(matchOn);
    } else {
      this.matchValue = null;
    }
    if (rule.getMaxDuration().isPresent()) {
      this.maxDuration = rule.getMaxDuration().get().toMilliseconds();
      this.firstMillis = notification.getCreatedAt().getMillis();
    } else {
      this.maxDuration = null;
      this.firstMillis = null;
    }
  }

  /**
   * Checks whether the notification contains the "match-on" field and the value matches the same
   * value as the first notification in the matcher.
   * 
   * @param notification Notification to match
   * @return true if the notification matches, otherwise false
   */
  public boolean checkMatch(@Nonnull final Notification notification) {
    Preconditions.checkNotNull(notification);
    if (!rule.getMatchOn().isPresent()) {
      return true;
    }

    if (matchOn == null || matchValue == null) {
      return false;
    }

    final Map<String, String> properties = notification.getProperties();
    return properties.containsKey(matchOn) && Objects.equals(properties.get(matchOn), matchValue);
  }

  /**
   * Check whether this matcher has reached "max-size" or not.
   *
   * @return true if we can put more notifications in this matcher, otherwise false.
   */
  public boolean checkSize() {
    if (maxSize != null && notifications.size() >= maxSize) {
      return false;
    }
    return true;
  }

  /**
   * Check whether the given notification is within the "max-duration" for this matcher or not.
   *
   * @param notification Notification to check
   * @return true if the notification is within the maximum duration, otherwise false.
   */
  public boolean checkDuration(@Nonnull final Notification notification) {
    Preconditions.checkNotNull(notification);
    if (!rule.getMaxDuration().isPresent()) {
      return true;
    }

    if (maxDuration != null && firstMillis != null) {
      final long timestamp = notification.getCreatedAt().getMillis();
      final long delta = firstMillis - timestamp;
      if (delta >= 0 && delta <= maxDuration) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add the given notification into this matcher if the checks pass.
   *
   * @param notification Notification to add
   * @return true if the notification can be added, otherwise false.
   */
  public boolean add(@Nonnull final Notification notification) {
    Preconditions.checkNotNull(notification);
    if (checkSize() && checkDuration(notification) && checkMatch(notification)
        && !this.notification.equals(notification)) {
      return notifications.add(notification);
    }
    return false;
  }

  @VisibleForTesting
  protected boolean add(@Nonnull final Collection<Notification> notifications) {
    return this.notifications.addAll(notifications);
  }

  public long getId() {
    return notification.getId(0L);
  }

  public Notification getNotification() {
    if (notifications.size() < 1) {
      return notification;
    }
    return Notification.builder().fromNotification(notification).withNotifications(notifications)
        .build();
  }

  @Override
  public int compareTo(final Matcher that) {
    return ComparisonChain.start()
        .compare(this.getId(), that.getId(), Ordering.natural().reverse()).result();
  }
}
