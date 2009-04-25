package com.wideplay.warp.persist.spi;

import com.google.inject.Module;

/**
 * {@link com.google.inject.Module} returned by
 * a {@link com.wideplay.warp.persist.PersistenceStrategy}.
 * 
 * @author Robbie Vanbrabant
 */
public interface PersistenceModule extends Module {
    /**
     * Visit a persistence module and collect information used to configure
     * services that Warp Persist offers, like managing the unit of work.
     * @param visitor invoked by the receiving module to set information relevant to Warp Persist
     */
    void visit(PersistenceModuleVisitor visitor);
}
