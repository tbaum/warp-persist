/**
 * Copyright (C) 2008 Wideplay Interactive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wideplay.codemonkey.web;

import com.google.inject.Inject;
import com.wideplay.codemonkey.model.SourceArtifact;
import com.wideplay.warp.annotations.OnEvent;
import com.wideplay.warp.annotations.URIMapping;
import com.wideplay.warp.annotations.event.PostRender;
import com.wideplay.warp.annotations.event.PreRender;
import com.wideplay.warp.persist.Transactional;

import javax.persistence.EntityManager;

/**
 * Created with IntelliJ IDEA.
 * On: 29/04/2007
 *
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 * @since 1.0
 */
@URIMapping("/jpa")
public class StartJpa {

    private String message;
    @Inject
    private EntityManager em;
    private SourceArtifact artifact;


    public StartJpa() {
    }

    @OnEvent @PreRender
    @Transactional
    void pre() {
        message = "Is txn active? " + em.getTransaction().isActive();
        try {
            em.persist(artifact = new SourceArtifact());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @OnEvent @PostRender
    @Transactional void post() {
        boolean b = false;
        try {
            b = em.contains(artifact);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Was object in the same em? " + b);
    }

    public String getMessage() {
        return message;
    }
}
