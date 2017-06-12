// Copyright 2017, Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.adwords.scripts.solutions.linkchecker.datastore;

import com.google.adwords.scripts.solutions.linkchecker.model.Account;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.BatchSubOperation;
import com.google.adwords.scripts.solutions.linkchecker.model.Settings;
import com.google.adwords.scripts.solutions.linkchecker.model.SharedKey;
import com.google.adwords.scripts.solutions.linkchecker.urlcheck.UrlCheckStatus;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class for interacting with Datastore through Objectify, to load/save/manage link checking jobs.
 */
public class Datastore {
  static {
    ObjectifyService.register(BatchOperation.class);
    ObjectifyService.register(BatchSubOperation.class);
    ObjectifyService.register(UrlCheckStatus.class);
    ObjectifyService.register(SharedKey.class);
    ObjectifyService.register(Settings.class);
    ObjectifyService.begin();
  }

  public Datastore() {}

  public static Objectify ofy() {
    return ObjectifyService.ofy();
  }

  /**
   * Saves a {@code BatchOperation} object to Datastore.
   *
   * @param op The BatchOperation object to save.
   */
  public void saveBatchOperation(BatchOperation op) {
    ofy().save().entity(op).now();
  }

  /**
   * Retrieves a {@code BatchOperation} given an account ID and the ID of the
   * {@code BatchOperation}.
   *
   * @param accountId The account ID to retrieve the {@code BatchOperation} for.
   * @param id The ID of the {@code BatchOperation}.
   * @return The loaded {@code BatchOperation}.
   */
  public BatchOperation loadBatchOperation(String accountId, String id) {
    Key accountKey = Key.create(Account.class, accountId);
    Key opKey = Key.create(accountKey, BatchOperation.class, id);
    return (BatchOperation) ofy().load().group(BatchSubOperation.class).key(opKey).safe();
  }

  /**
   * Retrieves a list of {@code BatchOperation} objects for a given account ID, optionally matching
   * a status.
   *
   * @param accountId The account ID to list {@code BatchOperation}s for.
   * @return A list of {@code BatchOperation}s.
   */
  public List<BatchOperation> listBatchOperations(String accountId) {
    Key accountKey = Key.create(Account.class, accountId);
    return ofy().load().type(BatchOperation.class).ancestor(accountKey).list();
  }

  /**
   * Deletes a specified {@code BatchOperation}.
   *
   * @param accountId The account ID for which the {@code BatchOperation} is for.
   * @param id The ID of the {@code BatchOperation} to delete.
   */
  public void deleteBatchOperation(String accountId, String id) {
    Key accountKey = Key.create(Account.class, accountId);
    Key opKey = Key.create(accountKey, BatchOperation.class, id);
    ofy().delete().keys(ofy().load().ancestor(opKey).keys().list()).now();
  }

  /**
   * Retrieve a list of BatchOperations that were created before a specified date.
   *
   * @param boundaryDate The date representing the upper bound for returned {@code BatchOperation}s.
   * @return A list of BatchOperations that were created before {@code boundaryDate}.
   */
  public List<BatchOperation> listHistoricBatchOperations(Date boundaryDate) {
    return ofy().load().type(BatchOperation.class).filter("created <", boundaryDate).list();
  }

  /**
   * Retrieves a {@code BatchSubOperation} for a specified ID. Each {@code BatchOperation} consists
   * of 1 or more {@code BatchSubOperation}, and each {@code BatchSubOperation} is serviced through
   * 1 TaskQueue entry.
   *
   * @param accountId The account ID for which the {@code BatchSubOperation} is for.
   * @param parentId The ID of the owning {@code BatchOperation}.
   * @param id The ID of the {@code BatchSubOperation}.
   * @return The loaded {@code BatchSubOperation}.
   */
  public BatchSubOperation loadBatchSubOperation(String accountId, String parentId, String id) {
    Key accountKey = Key.create(Account.class, accountId);
    Key parentKey = Key.create(accountKey, BatchOperation.class, parentId);
    Key opKey = Key.create(parentKey, BatchSubOperation.class, id);
    return (BatchSubOperation) ofy().load().key(opKey).safe();
  }

  /**
   * Decrements the count for the number of {@code BatchSubOperation}s still processing for a given
   * {@code BatchOperation}. This is executed as a single transaction, as should the count reach
   * zero, the {@code BatchOperation} is then marked as Complete. Therefore it is important that
   * only one decrement is taking place at a given time.
   *
   * @param accountId The account ID for the owning {@code BatchOperation}.
   * @param id The ID of the {@code BatchOperation}.
   */
  public void decrementSubOperationsRemaining(final String accountId, final String id) {
    ofy()
        .transact(
            new VoidWork() {
              @Override
              public void vrun() {
                Key accountKey = Key.create(Account.class, accountId);
                Key key = Key.create(accountKey, BatchOperation.class, id);
                BatchOperation op = (BatchOperation) ofy().load().key(key).now();
                op.decrementRemainingSubOperations();
                ofy().save().entity(op);
              }
            });
  }

  /**
   * Saves a {@code BatchOperation} and children
   *
   * @param op The {@code BatchOperation}
   * @param subOps A list of {@code BatchOperation}s.
   */
  public void saveBatchOperationAndChildren(BatchOperation op, List<BatchSubOperation> subOps) {
    List<Object> items = new ArrayList<>();
    items.addAll(subOps);
    items.add(op);
    // Save in one transaction
    ofy().save().entities(items).now();
  }
  
  /**
   * Saves a {@code BatchSubOperation}.
   *
   * @param subOp The {@code BatchSubOperation} to save.
   */
  public void saveBatchSubOperation(BatchSubOperation subOp) {
    ofy().save().entity(subOp).now();
  }

  /**
   * Saves a list of {@code BatchSubOperation}s.
   *
   * @param subOps The {@code BatchSubOperation}s to save.
   */
  public void saveBatchSubOperations(List<BatchSubOperation> subOps) {
    ofy().save().entities(subOps).now();
  }

  /**
   * Retrieves the shared key from the Datastore.
   *
   * @return The {@code SharedKey} object.
   */
  public SharedKey getKey() {
    Key keyId = Key.create(SharedKey.class, "key");
    return (SharedKey) ofy().load().key(keyId).safe();
  }

  /**
   * Creates and saves a shared key.
   *
   * @return The created {@code SharedKey} object.
   */
  public SharedKey createKey() {
    SharedKey key = SharedKey.createRandom();
    ofy().save().entity(key).now();
    return key;
  }
  
  /**
   * Retrieves the user-modifiable settings from the Datastore.
   *
   * @return The {@code Settings} object.
   */
  public Settings getSettings() {
    Key keyId = Key.create(Settings.class, "settings");
    return (Settings) ofy().load().key(keyId).safe();
  }

  /**
   * Creates and saves the user-modifiable settings with default values in the DataStore.
   *
   * @return The created {@code Settings} object.
   */
  public Settings createDefaultSettings() {
    Settings key = Settings.createDefaultSettings();
    ofy().save().entity(key).now();
    return key;
  }

  /**
   * Updates the user-modifiable settings in the Datastore.
   *
   * @param settings The settings object to write to Datastore.
   * @return The {@code Settings} object.
   */
  public Settings updateSettings(Settings settings) {
    ofy().save().entity(settings).now();
    return settings;
  }
}
