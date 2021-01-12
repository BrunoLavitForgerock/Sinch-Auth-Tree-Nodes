/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */


package com.sinch.authNode;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * A node that checks to see if zero-page login headers have specified username and shared key
 * for this request.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SinchAuthNode.Config.class)
public class SinchAuthNode extends SingleOutcomeNode {

    static final String USER_PHONE_KEY = "phoneNumber";

    private static final String BUNDLE = "com/sinch/authNode/SinchAuthNode";

    private final Logger logger = LoggerFactory.getLogger(SinchAuthNode.class);
    private final Config config;

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public SinchAuthNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        String userPhone = context.sharedState.get(USER_PHONE_KEY).asString();
        if (userPhone == null) {
            if (context.hasCallbacks() && context.getCallback(NameCallback.class).isPresent()) {
                userPhone = context.getCallback(NameCallback.class).get().getName();
                logger.debug("User phone is " + userPhone);
            } else {
                return Action.send(Arrays.asList(
                        new TextOutputCallback(TextOutputCallback.INFORMATION, bundle.getString("callback.phoneNumberText")),
                        new NameCallback(bundle.getString("callback.phoneNumber"))
                )).build();
            }
        }
        return goToNext()
                .replaceSharedState(context.sharedState.put(USER_PHONE_KEY, userPhone))
                .build();
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 1, validators = {RequiredValueValidator.class})
        default String appHash() {
            return "";
        }
    }

}