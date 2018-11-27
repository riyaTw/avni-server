package org.openchs.dao;

import org.openchs.domain.Program;
import org.openchs.domain.ProgramOrganisationConfig;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "programConfig", path = "programConfig")
public interface ProgramOrganisationConfigRepository extends ReferenceDataRepository<ProgramOrganisationConfig>, FindByLastModifiedDateTime<ProgramOrganisationConfig> {
    ProgramOrganisationConfig findByProgram(Program program);

    default ProgramOrganisationConfig findByName(String name) {
        throw new UnsupportedOperationException("No field 'name' in ProgramOrganisationConfig");
    }

    default ProgramOrganisationConfig findByNameIgnoreCase(String name) {
        throw new UnsupportedOperationException("No field 'name' in ProgramOrganisationConfig");
    }
}
