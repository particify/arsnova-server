package net.particify.arsnova.core.persistence.couchdb;

/**
 * For use with {@link MangoCouchDbCrudRepository}s that have their own index
 * creation logic which needs to be run during DB initialization.
 */
public interface MangoIndexInitializer {
  void createIndexes();
}
