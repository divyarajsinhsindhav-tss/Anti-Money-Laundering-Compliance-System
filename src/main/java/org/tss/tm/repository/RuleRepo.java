package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Rule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleRepo extends JpaRepository<Rule, UUID> {
    Optional<Rule>  findByRuleCode(String ruleCode);
    List<Rule> findAllByRuleCodeIn(List<String> ruleCodes);
}
