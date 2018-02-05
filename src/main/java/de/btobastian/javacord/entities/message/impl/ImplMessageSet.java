package de.btobastian.javacord.entities.message.impl;

import com.fasterxml.jackson.databind.JsonNode;
import de.btobastian.javacord.ImplDiscordApi;
import de.btobastian.javacord.entities.DiscordEntity;
import de.btobastian.javacord.entities.channels.TextChannel;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageSet;
import de.btobastian.javacord.utils.rest.RestEndpoint;
import de.btobastian.javacord.utils.rest.RestMethod;
import de.btobastian.javacord.utils.rest.RestRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The implementation of {@link MessageSet}.
 */
public class ImplMessageSet implements MessageSet {

    /**
     * A read-only navigable set with all messages to let the JDK do the dirty work.
     */
    private final NavigableSet<Message> messages;

    /**
     * Creates a new message set.
     *
     * @param messages The messages to be contained in this set.
     */
    public ImplMessageSet(NavigableSet<Message> messages) {
        this.messages = Collections.unmodifiableNavigableSet(messages);
    }

    /**
     * Creates a new message set.
     *
     * @param messages The messages to be contained in this set.
     */
    public ImplMessageSet(Collection<Message> messages) {
        this(new TreeSet<>(messages));
    }

    /**
     * Creates a new message set.
     *
     * @param messages The messages to be contained in this set.
     */
    public ImplMessageSet(Message... messages) {
        this(Arrays.asList(messages));
    }

    /**
     * Gets up to a given amount of messages in the given channel from the newer end.
     *
     * @param channel The channel of the messages.
     * @param limit The limit of messages to get.
     * @return The messages.
     * @see #getMessagesAsStream(TextChannel)
     */
    public static CompletableFuture<MessageSet> getMessages(TextChannel channel, int limit) {
        return getMessages(channel, limit, -1, -1);
    }

    /**
     * Gets messages in the given channel from the newer end until one that meets the given condition is found.
     * If no message matches the condition, an empty set is returned.
     *
     * @param channel The channel of the messages.
     * @param condition The abort condition for when to stop retrieving messages.
     * @return The messages.
     * @see #getMessagesAsStream(TextChannel)
     */
    public static CompletableFuture<MessageSet> getMessagesUntil(TextChannel channel, Predicate<Message> condition) {
        return getMessagesUntil(channel, condition, -1, -1);
    }

    /**
     * Gets a stream of messages in the given channel sorted from newest to oldest.
     * <p>
     * The messages are retrieved in batches synchronously from Discord,
     * so consider not using this method from a listener directly.
     *
     * @param channel The channel of the messages.
     * @return The stream.
     * @see #getMessages(TextChannel, int)
     */
    public static Stream<Message> getMessagesAsStream(TextChannel channel) {
        return getMessagesAsStream(channel, -1, -1);
    }

    /**
     * Gets up to a given amount of messages in the given channel before a given message in any channel.
     *
     * @param channel The channel of the messages.
     * @param limit The limit of messages to get.
     * @param before Get messages before the message with this id.
     * @return The messages.
     * @see #getMessagesBeforeAsStream(TextChannel, long)
     */
    public static CompletableFuture<MessageSet> getMessagesBefore(TextChannel channel, int limit, long before) {
        return getMessages(channel, limit, before, -1);
    }

    /**
     * Gets messages in the given channel before a given message in any channel until one that meets the given
     * condition is found.
     * If no message matches the condition, an empty set is returned.
     *
     * @param channel The channel of the messages.
     * @param condition The abort condition for when to stop retrieving messages.
     * @param before Get messages before the message with this id.
     * @return The messages.
     * @see #getMessagesBeforeAsStream(TextChannel, long)
     */
    public static CompletableFuture<MessageSet> getMessagesBeforeUntil(
            TextChannel channel, Predicate<Message> condition, long before) {
        return getMessagesUntil(channel, condition, before, -1);
    }

    /**
     * Gets a stream of messages in the given channel before a given message in any channel sorted from newest to
     * oldest.
     * <p>
     * The messages are retrieved in batches synchronously from Discord,
     * so consider not using this method from a listener directly.
     *
     * @param channel The channel of the messages.
     * @param before Get messages before the message with this id.
     * @return The stream.
     * @see #getMessagesBefore(TextChannel, int, long)
     */
    public static Stream<Message> getMessagesBeforeAsStream(TextChannel channel, long before) {
        return getMessagesAsStream(channel, before, -1);
    }

    /**
     * Gets up to a given amount of messages in the given channel after a given message in any channel.
     *
     * @param channel The channel of the messages.
     * @param limit The limit of messages to get.
     * @param after Get messages after the message with this id.
     * @return The messages.
     * @see #getMessagesAfterAsStream(TextChannel, long)
     */
    public static CompletableFuture<MessageSet> getMessagesAfter(TextChannel channel, int limit, long after) {
        return getMessages(channel, limit, -1, after);
    }

    /**
     * Gets messages in the given channel after a given message in any channel until one that meets the given condition
     * is found.
     * If no message matches the condition, an empty set is returned.
     *
     * @param channel The channel of the messages.
     * @param condition The abort condition for when to stop retrieving messages.
     * @param after Get messages after the message with this id.
     * @return The messages.
     * @see #getMessagesAfterAsStream(TextChannel, long)
     */
    public static CompletableFuture<MessageSet> getMessagesAfterUntil(
            TextChannel channel, Predicate<Message> condition, long after) {
        return getMessagesUntil(channel, condition, -1, after);
    }

    /**
     * Gets a stream of messages in the given channel after a given message in any channel sorted from oldest to newest.
     * <p>
     * The messages are retrieved in batches synchronously from Discord,
     * so consider not using this method from a listener directly.
     *
     * @param channel The channel of the messages.
     * @param after Get messages after the message with this id.
     * @return The stream.
     * @see #getMessagesAfter(TextChannel, int, long)
     */
    public static Stream<Message> getMessagesAfterAsStream(TextChannel channel, long after) {
        return getMessagesAsStream(channel, -1, after);
    }

    /**
     * Gets up to a given amount of messages in the given channel around a given message in any channel.
     * The given message will be part of the result in addition to the messages around if it was sent in the given
     * channel and does not count towards the limit.
     * Half of the messages will be older than the given message and half of the messages will be newer.
     * If there aren't enough older or newer messages, the actual amount of messages will be less than the given limit.
     * It's also not guaranteed to be perfectly balanced.
     *
     * @param channel The channel of the messages.
     * @param limit The limit of messages to get.
     * @param around Get messages around the message with this id.
     * @return The messages.
     * @see #getMessagesAroundAsStream(TextChannel, long)
     */
    public static CompletableFuture<MessageSet> getMessagesAround(TextChannel channel, int limit, long around) {
        CompletableFuture<MessageSet> future = new CompletableFuture<>();
        channel.getApi().getThreadPool().getExecutorService().submit(() -> {
            try {
                // calculate the half limit.
                int halfLimit = limit / 2;

                // get the newer half
                MessageSet newerMessages = getMessagesAfter(channel, halfLimit, around).join();

                // get the older half + around message
                MessageSet olderMessages = getMessagesBefore(channel, halfLimit + 1, around + 1).join();

                // remove the oldest message if the around message is not part of the result while there is a result,
                // for example because the around message was from a different channel
                if (olderMessages.getNewestMessage()
                        .map(DiscordEntity::getId)
                        .map(id -> id != around)
                        .orElse(false)) {
                    olderMessages = olderMessages.tailSet(
                            olderMessages.getOldestMessage().orElseThrow(AssertionError::new),
                            false);
                }

                // combine the messages into one collection
                Collection<Message> messages = Stream
                        .of(olderMessages, newerMessages)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

                // we are done
                future.complete(new ImplMessageSet(messages));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Gets messages in the given channel around a given message in any channel until one that meets the given
     * condition is found. If no message matches the condition, an empty set is returned.
     * The given message will be part of the result in addition to the messages around if it was sent in the given
     * channel and is matched against the condition and will abort retrieval.
     * Half of the messages will be older than the given message and half of the messages will be newer.
     * If there aren't enough older or newer messages, the halves will not be same-sized.
     * It's also not guaranteed to be perfectly balanced.
     *
     * @param channel The channel of the messages.
     * @param condition The abort condition for when to stop retrieving messages.
     * @param around Get messages around the message with this id.
     *
     * @return The messages.
     */
    public static CompletableFuture<MessageSet> getMessagesAroundUntil(
            TextChannel channel, Predicate<Message> condition, long around) {
        CompletableFuture<MessageSet> future = new CompletableFuture<>();
        channel.getApi().getThreadPool().getExecutorService().submit(() -> {
            try {
                List<Message> messages = new ArrayList<>();
                Optional<Message> untilMessage =
                        getMessagesAroundAsStream(channel, around).peek(messages::add).filter(condition).findFirst();

                future.complete(new ImplMessageSet(untilMessage
                                                           .map(message -> messages)
                                                           .orElse(Collections.emptyList())));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Gets a stream of messages in the given channel around a given message in any channel.
     * The first message in the stream will be the given message if it was sent in the given channel.
     * After that you will always get an older message and a newer message alternating as long as on both sides
     * messages are available. If only on one side further messages are available, only those are delivered further on.
     * It's not guaranteed to be perfectly balanced.
     * <p>
     * The messages are retrieved in batches synchronously from Discord,
     * so consider not using this method from a listener directly.
     *
     * @param channel The channel of the messages.
     * @param around Get messages around the message with this id.
     * @return The stream.
     * @see #getMessagesAround(TextChannel, int, long)
     */
    public static Stream<Message> getMessagesAroundAsStream(TextChannel channel, long around) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Message>() {
            private final ImplDiscordApi api = ((ImplDiscordApi) channel.getApi());
            private final AtomicBoolean firstBatch = new AtomicBoolean(true);
            private final AtomicBoolean nextIsOlder = new AtomicBoolean();
            private long olderReferenceMessageId = around;
            private long newerReferenceMessageId = around - 1;
            private final List<JsonNode> olderMessageJsons = Collections.synchronizedList(new ArrayList<>());
            private final List<JsonNode> newerMessageJsons = Collections.synchronizedList(new ArrayList<>());
            private final AtomicBoolean hasMoreOlderMessages = new AtomicBoolean(true);
            private final AtomicBoolean hasMoreNewerMessages = new AtomicBoolean(true);

            private void ensureMessagesAvailable() {
                if (olderMessageJsons.isEmpty() && hasMoreOlderMessages.get()) {
                    synchronized (olderMessageJsons) {
                        if (olderMessageJsons.isEmpty() && hasMoreOlderMessages.get()) {
                            olderMessageJsons.addAll(requestAsSortedJsonNodes(
                                    channel,
                                    100,
                                    olderReferenceMessageId,
                                    -1,
                                    true
                            ));
                            if (olderMessageJsons.isEmpty()) {
                                hasMoreOlderMessages.set(false);
                            } else {
                                olderReferenceMessageId =
                                        olderMessageJsons.get(olderMessageJsons.size() - 1).get("id").asLong();
                            }
                        }
                    }
                }
                if (newerMessageJsons.isEmpty() && hasMoreNewerMessages.get()) {
                    synchronized (newerMessageJsons) {
                        if (newerMessageJsons.isEmpty() && hasMoreNewerMessages.get()) {
                            newerMessageJsons.addAll(requestAsSortedJsonNodes(
                                    channel,
                                    100,
                                    -1,
                                    newerReferenceMessageId,
                                    false
                            ));
                            if (newerMessageJsons.isEmpty()) {
                                hasMoreNewerMessages.set(false);
                            } else {
                                newerReferenceMessageId =
                                        newerMessageJsons.get(newerMessageJsons.size() - 1).get("id").asLong();
                                if (firstBatch.getAndSet(false)) {
                                    nextIsOlder.set(newerMessageJsons.get(0).get("id").asLong() != around);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public boolean hasNext() {
                ensureMessagesAvailable();
                return !(olderMessageJsons.isEmpty() && newerMessageJsons.isEmpty());
            }

            @Override
            public Message next() {
                ensureMessagesAvailable();
                boolean nextIsOlder = this.nextIsOlder.get();
                this.nextIsOlder.set(!nextIsOlder);
                JsonNode messageJson =
                        ((nextIsOlder && !olderMessageJsons.isEmpty()) || newerMessageJsons.isEmpty())
                        ? olderMessageJsons.remove(0)
                        : newerMessageJsons.remove(0);
                return api.getOrCreateMessage(channel, messageJson);
            }
        }, Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.CONCURRENT), false);
    }

    /**
     * Gets messages in the given channel until one that meets the given condition is found.
     * If no message matches the condition, an empty set is returned.
     *
     * @param channel The channel of the messages.
     * @param condition The abort condition for when to stop retrieving messages.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     *
     * @return The messages.
     */
    private static CompletableFuture<MessageSet> getMessagesUntil(
            TextChannel channel, Predicate<Message> condition, long before, long after) {
        CompletableFuture<MessageSet> future = new CompletableFuture<>();
        channel.getApi().getThreadPool().getExecutorService().submit(() -> {
            try {
                List<Message> messages = new ArrayList<>();
                Optional<Message> untilMessage =
                        getMessagesAsStream(channel, before, after).peek(messages::add).filter(condition).findFirst();

                future.complete(new ImplMessageSet(untilMessage
                                                           .map(message -> messages)
                                                           .orElse(Collections.emptyList())));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Gets up to a given amount of messages in the given channel.
     *
     * @param channel The channel of the messages.
     * @param limit The limit of messages to get.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     *
     * @return The messages.
     * @see #getMessagesAsStream(TextChannel, long, long)
     */
    private static CompletableFuture<MessageSet> getMessages(TextChannel channel, int limit, long before, long after) {
        CompletableFuture<MessageSet> future = new CompletableFuture<>();
        channel.getApi().getThreadPool().getExecutorService().submit(() -> {
            try {
                // get the initial batch with the first <= 100 messages
                int initialBatchSize = ((limit % 100) == 0) ? 100 : limit % 100;
                MessageSet initialMessages = requestAsMessages(channel, initialBatchSize, before, after);

                // limit <= 100 => initial request got all messages
                // initialMessages is empty => READ_MESSAGE_HISTORY permission is denied or no more messages available
                if ((limit <= 100) || initialMessages.isEmpty()) {
                    future.complete(initialMessages);
                    return;
                }

                // calculate the amount and direction of remaining message to get
                // this will be a multiple of 100 and at least 100
                int remainingMessages = limit - initialBatchSize;
                int steps = remainingMessages / 100;
                // "before" is set or both are not set
                boolean older = (before != -1) || (after == -1);
                boolean newer = after != -1;

                // get remaining messages
                List<MessageSet> messageSets = new ArrayList<>();
                MessageSet lastMessages = initialMessages;
                messageSets.add(lastMessages);
                for (int step = 0; step < steps; ++step) {
                    lastMessages = requestAsMessages(channel,
                                                     100,
                                                     lastMessages.getOldestMessage()
                                                             .filter(message -> older)
                                                             .map(DiscordEntity::getId)
                                                             .orElse(-1L),
                                                     lastMessages.getNewestMessage()
                                                             .filter(message -> newer)
                                                             .map(DiscordEntity::getId)
                                                             .orElse(-1L));

                    // no more messages available
                    if (lastMessages.isEmpty()) {
                        break;
                    }

                    messageSets.add(lastMessages);
                }

                // combine the message sets
                future.complete(new ImplMessageSet(messageSets.stream()
                                                           .flatMap(Collection::stream)
                                                           .collect(Collectors.toList())));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Gets a stream of messages in the given channel sorted from newest to oldest.
     * <p>
     * The messages are retrieved in batches synchronously from Discord,
     * so consider not using this method from a listener directly.
     *
     * @param channel The channel of the messages.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     *
     * @return The stream.
     * @see #getMessages(TextChannel, int, long, long)
     */
    private static Stream<Message> getMessagesAsStream(TextChannel channel, long before, long after) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Message>() {
            private final ImplDiscordApi api = ((ImplDiscordApi) channel.getApi());
            // before was set or both were not set
            private final boolean older = (before != -1) || (after == -1);
            private final boolean newer = after != -1;
            private long referenceMessageId = older ? before : after;
            private final List<JsonNode> messageJsons = Collections.synchronizedList(new ArrayList<>());

            private void ensureMessagesAvailable() {
                if (messageJsons.isEmpty()) {
                    synchronized (messageJsons) {
                        if (messageJsons.isEmpty()) {
                            messageJsons.addAll(requestAsSortedJsonNodes(
                                    channel,
                                    100,
                                    older ? referenceMessageId : -1,
                                    newer ? referenceMessageId : -1,
                                    older
                            ));
                            if (!messageJsons.isEmpty()) {
                                referenceMessageId = messageJsons.get(messageJsons.size() - 1).get("id").asLong();
                            }
                        }
                    }
                }
            }

            @Override
            public boolean hasNext() {
                ensureMessagesAvailable();
                return !messageJsons.isEmpty();
            }

            @Override
            public Message next() {
                ensureMessagesAvailable();
                return api.getOrCreateMessage(channel, messageJsons.remove(0));
            }
        }, Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.CONCURRENT), false);
    }

    /**
     * Requests the messages from Discord.
     *
     * @param channel The channel of which to get messages from.
     * @param limit The limit of messages to get.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     * @return The messages.
     */
    private static MessageSet requestAsMessages(TextChannel channel, int limit, long before, long after) {
        ImplDiscordApi api = (ImplDiscordApi) channel.getApi();
        return new ImplMessageSet(
                requestAsJsonNodes(channel, limit, before, after).stream()
                        .map(jsonNode -> api.getOrCreateMessage(channel, jsonNode))
                        .collect(Collectors.toList()));
    }

    /**
     * Requests the messages from Discord, sorted by their id.
     *
     * @param channel The channel of which to get messages from.
     * @param limit The limit of messages to get.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     * @param reversed If {@code true}, get from oldest to newest, otherwise from newest to oldest.
     * @return The JSON nodes.
     */
    private static List<JsonNode> requestAsSortedJsonNodes(
            TextChannel channel, int limit, long before, long after, boolean reversed) {
        List<JsonNode> messageJsonNodes = requestAsJsonNodes(channel, limit, before, after);
        Comparator<JsonNode> idComparator = Comparator.comparingLong(jsonNode -> jsonNode.get("id").asLong());
        messageJsonNodes.sort(reversed ? idComparator.reversed() : idComparator);
        return messageJsonNodes;
    }

    /**
     * Requests the messages from Discord.
     *
     * @param channel The channel of which to get messages from.
     * @param limit The limit of messages to get.
     * @param before Get messages before the message with this id.
     * @param after Get messages after the message with this id.
     * @return The JSON nodes.
     */
    private static List<JsonNode> requestAsJsonNodes(TextChannel channel, int limit, long before, long after) {
        RestRequest<List<JsonNode>> restRequest =
                new RestRequest<List<JsonNode>>(channel.getApi(), RestMethod.GET, RestEndpoint.MESSAGE)
                .setUrlParameters(String.valueOf(channel.getId()));

        if (limit != -1) {
            restRequest.addQueryParameter("limit", String.valueOf(limit));
        }
        if (before != -1) {
            restRequest.addQueryParameter("before", String.valueOf(before));
        }
        if (after != -1) {
            restRequest.addQueryParameter("after", String.valueOf(after));
        }

        return restRequest.execute(result -> {
            List<JsonNode> messageJsonNodes = new ArrayList<>();
            result.getJsonBody().iterator().forEachRemaining(messageJsonNodes::add);
            return messageJsonNodes;
        }).join();
    }

    @Override
    public Message lower(Message message) {
        return messages.lower(message);
    }

    @Override
    public Message floor(Message message) {
        return messages.floor(message);
    }

    @Override
    public Message ceiling(Message message) {
        return messages.ceiling(message);
    }

    @Override
    public Message higher(Message message) {
        return messages.higher(message);
    }

    @Override
    public Message pollFirst() {
        return messages.pollFirst();
    }

    @Override
    public Message pollLast() {
        return messages.pollLast();
    }

    @Override
    public int size() {
        return messages.size();
    }

    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return messages.contains(o);
    }

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    @Override
    public Object[] toArray() {
        return messages.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return messages.toArray(a);
    }

    @Override
    public boolean add(Message message) {
        return messages.add(message);
    }

    @Override
    public boolean remove(Object o) {
        return messages.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return messages.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Message> c) {
        return messages.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return messages.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return messages.removeAll(c);
    }

    @Override
    public void clear() {
        messages.clear();
    }

    @Override
    public NavigableSet<Message> descendingSet() {
        return messages.descendingSet();
    }

    @Override
    public Iterator<Message> descendingIterator() {
        return messages.descendingIterator();
    }

    @Override
    public MessageSet subSet(Message fromElement, boolean fromInclusive, Message toElement, boolean toInclusive) {
        return new ImplMessageSet(messages.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public MessageSet headSet(Message toElement, boolean inclusive) {
        return new ImplMessageSet(messages.headSet(toElement, inclusive));
    }

    @Override
    public MessageSet tailSet(Message fromElement, boolean inclusive) {
        return new ImplMessageSet(messages.tailSet(fromElement, inclusive));
    }

    @Override
    public Comparator<? super Message> comparator() {
        return messages.comparator();
    }

    @Override
    public MessageSet subSet(Message fromElement, Message toElement) {
        return new ImplMessageSet(messages.subSet(fromElement, toElement));
    }

    @Override
    public MessageSet headSet(Message toElement) {
        return new ImplMessageSet(messages.headSet(toElement));
    }

    @Override
    public MessageSet tailSet(Message fromElement) {
        return new ImplMessageSet(messages.tailSet(fromElement));
    }

    @Override
    public Message first() {
        return messages.first();
    }

    @Override
    public Message last() {
        return messages.last();
    }

}
