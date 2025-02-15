package org.dataloader;

import org.dataloader.annotations.PublicApi;
import org.dataloader.stats.Statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This allows data loaders to be registered together into a single place, so
 * they can be dispatched as one.  It also allows you to retrieve data loaders by
 * name from a central place
 */
@PublicApi
public class DataLoaderRegistry {
    protected final Map<String, DataLoader<?, ?>> dataLoaders = new ConcurrentHashMap<>();

    public DataLoaderRegistry() {
    }

    private DataLoaderRegistry(Builder builder) {
        this.dataLoaders.putAll(builder.dataLoaders);
    }


    /**
     * This will register a new dataloader
     *
     * @param key        the key to put the data loader under
     * @param dataLoader the data loader to register
     *
     * @return this registry
     */
    public DataLoaderRegistry register(String key, DataLoader<?, ?> dataLoader) {
        dataLoaders.put(key, dataLoader);
        return this;
    }

    /**
     * Computes a data loader if absent or return it if it was
     * already registered at that key.
     * <p>
     * Note: The entire method invocation is performed atomically,
     * so the function is applied at most once per key.
     *
     * @param key             the key of the data loader
     * @param mappingFunction the function to compute a data loader
     * @param <K>             the type of keys
     * @param <V>             the type of values
     *
     * @return a data loader
     */
    @SuppressWarnings("unchecked")
    public <K, V> DataLoader<K, V> computeIfAbsent(final String key,
                                                   final Function<String, DataLoader<?, ?>> mappingFunction) {
        return (DataLoader<K, V>) dataLoaders.computeIfAbsent(key, mappingFunction);
    }

    /**
     * This will combine all the current data loaders in this registry and all the data loaders from the specified registry
     * and return a new combined registry
     *
     * @param registry the registry to combine into this registry
     *
     * @return a new combined registry
     */
    public DataLoaderRegistry combine(DataLoaderRegistry registry) {
        DataLoaderRegistry combined = new DataLoaderRegistry();

        this.dataLoaders.forEach(combined::register);
        registry.dataLoaders.forEach(combined::register);
        return combined;
    }

    /**
     * @return the currently registered data loaders
     */
    public List<DataLoader<?, ?>> getDataLoaders() {
        return new ArrayList<>(dataLoaders.values());
    }

    /**
     * @return the currently registered data loaders as a map
     */
    public Map<String, DataLoader<?, ?>> getDataLoadersMap() {
        return new LinkedHashMap<>(dataLoaders);
    }

    /**
     * This will unregister a new dataloader
     *
     * @param key the key of the data loader to unregister
     *
     * @return this registry
     */
    public DataLoaderRegistry unregister(String key) {
        dataLoaders.remove(key);
        return this;
    }

    /**
     * Returns the dataloader that was registered under the specified key
     *
     * @param key the key of the data loader
     * @param <K> the type of keys
     * @param <V> the type of values
     *
     * @return a data loader or null if its not present
     */
    @SuppressWarnings("unchecked")
    public <K, V> DataLoader<K, V> getDataLoader(String key) {
        return (DataLoader<K, V>) dataLoaders.get(key);
    }

    /**
     * @return the keys of the data loaders in this registry
     */
    public Set<String> getKeys() {
        return new HashSet<>(dataLoaders.keySet());
    }

    /**
     * This will called {@link org.dataloader.DataLoader#dispatch()} on each of the registered
     * {@link org.dataloader.DataLoader}s
     */
    public void dispatchAll() {
        getDataLoaders().forEach(DataLoader::dispatch);
    }

    /**
     * Similar to {@link DataLoaderRegistry#dispatchAll()}, this calls {@link org.dataloader.DataLoader#dispatch()} on
     * each of the registered {@link org.dataloader.DataLoader}s, but returns the number of dispatches.
     *
     * @return total number of entries that were dispatched from registered {@link org.dataloader.DataLoader}s.
     */
    public int dispatchAllWithCount() {
        int sum = 0;
        for (DataLoader<?, ?> dataLoader : getDataLoaders()) {
            sum += dataLoader.dispatchWithCounts().getKeysCount();
        }
        return sum;
    }

    /**
     * @return The sum of all batched key loads that need to be dispatched from all registered
     * {@link org.dataloader.DataLoader}s
     */
    public int dispatchDepth() {
        int totalDispatchDepth = 0;
        for (DataLoader<?, ?> dataLoader : getDataLoaders()) {
            totalDispatchDepth += dataLoader.dispatchDepth();
        }
        return totalDispatchDepth;
    }

    /**
     * @return a combined set of statistics for all data loaders in this registry presented
     * as the sum of all their statistics
     */
    public Statistics getStatistics() {
        Statistics stats = new Statistics();
        for (DataLoader<?, ?> dataLoader : dataLoaders.values()) {
            stats = stats.combine(dataLoader.getStatistics());
        }
        return stats;
    }

    /**
     * @return A builder of {@link DataLoaderRegistry}s
     */
    public static Builder newRegistry() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, DataLoader<?, ?>> dataLoaders = new HashMap<>();

        /**
         * This will register a new dataloader
         *
         * @param key        the key to put the data loader under
         * @param dataLoader the data loader to register
         *
         * @return this builder for a fluent pattern
         */
        public Builder register(String key, DataLoader<?, ?> dataLoader) {
            dataLoaders.put(key, dataLoader);
            return this;
        }

        /**
         * This will combine together the data loaders in this builder with the ones
         * from a previous {@link DataLoaderRegistry}
         *
         * @param otherRegistry the previous {@link DataLoaderRegistry}
         *
         * @return this builder for a fluent pattern
         */
        public Builder registerAll(DataLoaderRegistry otherRegistry) {
            dataLoaders.putAll(otherRegistry.dataLoaders);
            return this;
        }

        /**
         * @return the newly built {@link DataLoaderRegistry}
         */
        public DataLoaderRegistry build() {
            return new DataLoaderRegistry(this);
        }
    }
}
