package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.common.enums.DataType;
import org.tss.tm.entity.common.BaseEntity;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "scenario_parameter_master", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScenarioParameterMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "parameter_id")
    private UUID parameterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private Rule rule;

    @Column(name = "parameter_key", nullable = false)
    private String parameterKey;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "data_type", nullable = false, columnDefinition = "data_type_enum")
    private DataType dataType;

    @Column(name = "default_value")
    private String defaultValue;

    @Builder.Default
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;
}
