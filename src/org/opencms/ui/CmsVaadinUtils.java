/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ui;

import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessages;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsMacroResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.declarative.Design;

/**
 * Vaadin utility functions.<p>
 *
 */
public final class CmsVaadinUtils {

    /** The logger of this class. */
    private static final Logger LOG = Logger.getLogger(CmsVaadinUtils.class);

    /**
     * Hidden default constructor for utility class.<p>
     */
    private CmsVaadinUtils() {

    }

    /**
     * Returns the path to the design template file of the given component.<p>
     *
     * @param component the component
     *
     * @return the path
     */
    public static String getDefaultDesignPath(Component component) {

        String className = component.getClass().getName();
        String designPath = className.replace(".", "/") + ".html";
        return designPath;
    }

    /**
     * Reads the declarative design for a component and localizes it using a messages object.<p>
     *
     * The design will need to be located in the same directory as the component's class and have '.html' as a file extension.
     *
     * @param component the component for which to read the design
     * @param messages the message bundle to use for localization
     * @param macros the macros to use on the HTML template
     */
    public static void readAndLocalizeDesign(Component component, CmsMessages messages, Map<String, String> macros) {

        String designPath = getDefaultDesignPath(component);
        readAndLocalizeDesign(component, designPath, messages, macros);
    }

    /**
     * Shows an alert box to the user with the given information, which will perform the given action after the user clicks on OK.<p>
     *
     * @param title the title
     * @param message the message
     *
     * @param callback the callback to execute after clicking OK
     */
    public static void showAlert(String title, String message, final Runnable callback) {

        final Window window = new Window();
        window.setModal(true);
        Panel panel = new Panel();
        panel.setCaption(title);
        panel.setWidth("500px");
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        panel.setContent(layout);
        layout.addComponent(new Label(message));
        Button okButton = new Button();
        okButton.addClickListener(new ClickListener() {

            /** The serial version id. */
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {

                window.close();
                callback.run();
            }
        });
        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.BOTTOM_RIGHT);
        okButton.setCaption("OK");
        window.setContent(panel);
        window.setClosable(false);
        window.setResizable(false);
        A_CmsUI.get().addWindow(window);

    }

    /**
     * Visits all descendants of a given component (including the component itself) and applies a predicate
     * to each.<p>
     *
     * If the predicate returns false for a component, no further descendants will be processed.<p>
     *
     * @param component the component
     * @param handler the predicate
     */
    public static void visitDescendants(Component component, Predicate<Component> handler) {

        List<Component> stack = Lists.newArrayList();
        stack.add(component);
        while (!stack.isEmpty()) {
            Component currentComponent = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            if (!handler.apply(currentComponent)) {
                return;
            }
            if (currentComponent instanceof HasComponents) {
                List<Component> children = Lists.newArrayList((HasComponents)currentComponent);
                Collections.reverse(children);
                stack.addAll(children);
            }
        }
    }

    /**
     * Reads the given design and resolves the given macros and localizations.<p>
    
     * @param component the component whose design to read
     * @param designPath the path to the design file
     * @param messages the message bundle to use for localization in the design (may be null)
     * @param macros other macros to substitute in the macro design (may be null)
     */
    protected static void readAndLocalizeDesign(
        Component component,
        String designPath,
        CmsMessages messages,
        Map<String, String> macros) {

        InputStream designStream = CmsVaadinUtils.class.getClassLoader().getResourceAsStream(designPath);
        if (designStream == null) {
            throw new IllegalArgumentException("Design not found: " + designPath);
        }
        try {
            byte[] designBytes = CmsFileUtil.readFully(designStream);
            final String encoding = "UTF-8";
            String design = new String(designBytes, encoding);
            CmsMacroResolver resolver = new CmsMacroResolver() {

                @Override
                public String getMacroValue(String macro) {

                    String result = super.getMacroValue(macro);
                    // The macro may contain quotes or angle brackets, so we need to escape the values for insertion into the design file
                    return CmsEncoder.escapeXml(result);

                }
            };

            if (macros != null) {
                for (Map.Entry<String, String> entry : macros.entrySet()) {
                    resolver.addMacro(entry.getKey(), entry.getValue());
                }
            }
            if (messages != null) {
                resolver.setMessages(messages);
            }
            String resolvedDesign = resolver.resolveMacros(design);
            Design.read(new ByteArrayInputStream(resolvedDesign.getBytes(encoding)), component);
        } catch (IOException e) {
            throw new RuntimeException("Could not read design: " + designPath, e);
        } finally {
            try {
                designStream.close();
            } catch (IOException e) {
                LOG.warn(e.getLocalizedMessage(), e);
            }
        }
    }

}
