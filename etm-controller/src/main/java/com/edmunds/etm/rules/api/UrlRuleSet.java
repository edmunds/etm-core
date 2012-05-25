/*
 * Copyright 2011 Edmunds.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.edmunds.etm.rules.api;

import com.edmunds.etm.management.api.MavenModule;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents an active rule set. <p/> Provides the methods for merging in new rules and ordering them for config
 * generation.
 *
 * @author David Trott
 */
public class UrlRuleSet {

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(UrlRuleSet.class);

    /**
     * The set of all rules.
     */
    private final Set<UrlRule> rules;

    /**
     * The alphabetically sorted list of "unblocked" rules. <p/> These are the highest priority rules that can be
     * outputted immediately.
     */
    private final SortedSet<UrlRule> unblockedRules;

    /**
     * The map of (blocking rule -> set of blocked rules).
     */
    private final Map<UrlRule, Set<UrlRule>> rulesBlockedByMe;

    /**
     * The reverse map of (blocked rule -> set of blocking rules).
     */
    private final Map<UrlRule, Set<UrlRule>> rulesBlockingMe;

    /**
     * Initializes rule set from a simple collection of rules.
     *
     * @param rules the rules to use to initialized the rule set.
     */
    public UrlRuleSet(Collection<UrlRule> rules) {
        this.rules = Sets.newHashSet(rules);
        this.unblockedRules = Sets.newTreeSet(AlphabeticUrlRuleComparator.INSTANCE);
        this.unblockedRules.addAll(rules);
        this.rulesBlockedByMe = Maps.newHashMap();
        this.rulesBlockingMe = Maps.newHashMap();
    }

    /**
     * Copy constructor. <p/> Needed because the merge operation is destructive to this objects state.
     *
     * @param other the other rule set to be copied.
     */
    public UrlRuleSet(UrlRuleSet other) {
        this.rules = Sets.newHashSet(other.rules);
        this.unblockedRules = new TreeSet<UrlRule>(other.unblockedRules);
        this.rulesBlockedByMe = deepCopyMapSet(other.rulesBlockedByMe);
        this.rulesBlockingMe = deepCopyMapSet(other.rulesBlockingMe);
    }

    /**
     * Creates a new object and merges the new rules into it.
     *
     * @param newRules the new rules to be merged.
     * @return the new rule set object or null if the merge failed.
     */
    public UrlRuleSet mergeRules(Collection<UrlRule> newRules) {
        final UrlRuleSet result = new UrlRuleSet(this);

        return result.mergeRulesInternal(newRules) ? result : null;
    }

    /**
     * Merges the new rules into the rule set managed by this object. <p/> CAUTION: This method is destructive to the
     * internal state of this object even if the merge fails.
     *
     * @param newRules the new rules to be merged.
     * @return true if the merge succeeds.
     */
    private boolean mergeRulesInternal(Collection<UrlRule> newRules) {
        for (UrlRule newRule : newRules) {

            boolean unblockedNewRule = true;

            for (UrlRule existingRule : rules) {
                final RuleComparison comparison = newRule.compareTo(existingRule);
                switch (comparison) {
                    case DISTINCT:
                        // No Action, the rules are DISTINCT move on to the next comparison.
                        break;
                    case HIGH_PRIORITY:
                        // newRule blocks existingRule
                        addToMapSet(newRule, existingRule, rulesBlockedByMe);
                        addToMapSet(existingRule, newRule, rulesBlockingMe);

                        // If an "unblocked" rules gets blocked by a new rule remove it from the "unblocked" list.
                        unblockedRules.remove(existingRule);
                        break;
                    case LOW_PRIORITY:
                        // existingRule blocks newRule
                        addToMapSet(newRule, existingRule, rulesBlockingMe);
                        addToMapSet(existingRule, newRule, rulesBlockedByMe);

                        // The existing rule has blocked the new rule.
                        unblockedNewRule = false;
                        break;
                    default:
                        logger.info("Conflict (" + comparison + ") detected between rules," +
                                " Existing: " + existingRule.getMavenModule() + "=[" + existingRule.getRule() + "]" +
                                " New: " + newRule.getMavenModule() + "=[" + newRule.getRule() + "]");

                        return false;
                }
            }

            if (unblockedNewRule) {
                unblockedRules.add(newRule);
            }
        }

        // We don't need to compare a rule with other rules from the same application.
        rules.addAll(newRules);

        return true;
    }

    public void deleteRules(MavenModule mavenModule) {
        final Iterator<UrlRule> it = rules.iterator();
        while (it.hasNext()) {
            final UrlRule rule = it.next();

            // Is this a rule for the same maven module
            if (rule.getMavenModule().equals(mavenModule)) {
                // It is so remove it.
                it.remove();
                // If its an unblocked rule remove it.
                unblockedRules.remove(rule);

                // Then find anyone this rule is blocking and anyone who is blocking it and unblock them.
                bidirectionalMapRemove(rule, rulesBlockedByMe, rulesBlockingMe, true);
                bidirectionalMapRemove(rule, rulesBlockingMe, rulesBlockedByMe, false);
            }
        }
    }

    public Set<UrlRule> orderRules() {
        final SortedSet<UrlRule> ready = new TreeSet<UrlRule>(unblockedRules);
        final Map<UrlRule, Set<UrlRule>> localRulesBlockedByMe = deepCopyMapSet(rulesBlockedByMe);
        final Map<UrlRule, Set<UrlRule>> localRulesBlockingMe = deepCopyMapSet(rulesBlockingMe);

        final Set<UrlRule> out = Sets.newLinkedHashSet();
        // The ready set is updated as part of the loop so we cannot use an iterator.
        while (!ready.isEmpty()) {
            final UrlRule current = ready.first();
            ready.remove(current);
            out.add(current);

            if (localRulesBlockedByMe.containsKey(current)) {
                unblockRules(current, ready, localRulesBlockedByMe, localRulesBlockingMe);
            }
        }

        // Have all rules been outputted?
        if (rules.size() != out.size()) {
            logger.error("Cyclic dependency detected in new rule set");
            return null;
        }

        return out;
    }

    /**
     * Returns {@code true} if this rule set contains the specified rule.
     *
     * @param r rule whose presence is to be tested.
     * @return true if this rule set contains the specified rule, false otherwise
     */
    public boolean contains(UrlRule r) {
        return rules.contains(r);
    }

    /**
     * Returns {@code true} if this rule set contains all of rules in the specified collection.
     *
     * @param c collection of rules to be checked for containment in this rule set
     * @return true if this rule set contains all of the rules in the specified collection
     */
    public boolean containsAll(Collection<UrlRule> c) {
        return rules.containsAll(c);
    }

    /**
     * Gets a map of blocked rules and the rule(s) that block them. <p/> For each map entry, the key is a blocked rule
     * and the value is the set of rules that blocks it.
     *
     * @return map of blocked rules (blocked rule -> set of blocking rules)
     */
    public Set<BlockedUrlRule> getBlockedRules() {
        Map<UrlRule, Set<UrlRule>> rulesMap = deepCopyMapSet(rulesBlockingMe);
        Set<BlockedUrlRule> blockedRules = new HashSet<BlockedUrlRule>(rulesMap.size());
        for (Map.Entry<UrlRule, Set<UrlRule>> entry : rulesMap.entrySet()) {
            blockedRules.add(new BlockedUrlRule(entry.getKey(), entry.getValue()));
        }
        return blockedRules;
    }

    private void unblockRules(
            UrlRule current, SortedSet<UrlRule> ready, Map<UrlRule,
            Set<UrlRule>> localRulesBlockedByMe, Map<UrlRule, Set<UrlRule>> localRulesBlockingMe) {

        final Set<UrlRule> blockedRules = localRulesBlockedByMe.remove(current);

        // For each rule that was blocked by the rule that was just outputted
        for (UrlRule blockedRule : blockedRules) {
            final Set<UrlRule> blockingRules = localRulesBlockingMe.get(blockedRule);
            blockingRules.remove(current);

            // Is the rule now unblocked?
            if (blockingRules.isEmpty()) {
                // If so it can be outputted as soon as it is alphabetically possible.
                ready.add(blockedRule);
            }
        }
    }

    private <K, V> void addToMapSet(K key, V value, Map<K, Set<V>> map) {
        Set<V> values = map.get(key);

        if (values == null) {
            values = Sets.newHashSet();
            map.put(key, values);
        }

        values.add(value);
    }

    private void bidirectionalMapRemove(
            UrlRule rule, Map<UrlRule, Set<UrlRule>> primary, Map<UrlRule, Set<UrlRule>> reverse,
            boolean addUnblocked) {

        final Set<UrlRule> others = primary.remove(rule);

        if (others != null) {
            for (UrlRule other : others) {
                final Set<UrlRule> otherSet = reverse.get(other);
                otherSet.remove(rule);

                // Did we remove the last entry in the set?
                if (otherSet.isEmpty()) {
                    if (addUnblocked) {
                        unblockedRules.add(other);
                    }
                    reverse.remove(other);
                }
            }
        }
    }

    private <K, V> Map<K, Set<V>> deepCopyMapSet(Map<K, Set<V>> map) {
        final Map<K, Set<V>> result = Maps.newHashMap();

        for (Map.Entry<K, Set<V>> entry : map.entrySet()) {
            result.put(entry.getKey(), Sets.newHashSet(entry.getValue()));
        }

        return result;
    }
}
