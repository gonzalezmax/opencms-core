/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH & Co. KG (http://www.alkacon.com)
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

package org.opencms.ui.apps.user;

import org.opencms.db.CmsUserSettings;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsIllegalStateException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsDefaultValidationHandler;
import org.opencms.security.CmsPasswordInfo;
import org.opencms.security.CmsRole;
import org.opencms.security.CmsSecurityException;
import org.opencms.security.I_CmsPasswordHandler;
import org.opencms.security.I_CmsPasswordSecurityEvaluator;
import org.opencms.security.I_CmsPasswordSecurityEvaluator.SecurityLevel;
import org.opencms.site.CmsSite;
import org.opencms.ui.A_CmsUI;
import org.opencms.ui.CmsVaadinUtils;
import org.opencms.ui.apps.CmsPageEditorConfiguration;
import org.opencms.ui.apps.CmsSitemapEditorConfiguration;
import org.opencms.ui.apps.I_CmsWorkplaceAppConfiguration;
import org.opencms.ui.apps.Messages;
import org.opencms.ui.components.CmsBasicDialog;
import org.opencms.ui.components.CmsUserDataFormLayout;
import org.opencms.ui.components.OpenCmsTheme;
import org.opencms.ui.components.fileselect.CmsPathSelectField;
import org.opencms.ui.dialogs.permissions.CmsPrincipalSelect;
import org.opencms.ui.dialogs.permissions.CmsPrincipalSelect.WidgetType;
import org.opencms.ui.login.CmsPasswordForm;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Class for the dialog to edit user settings.<p>
 */
public class CmsUserEditDialog extends CmsBasicDialog {

    /**
     * Validator for the eamil field.<p>
     */
    class EmailValidator implements Validator {

        /**vaadin serial id.*/
        private static final long serialVersionUID = 8943898736907290076L;

        /**
         * @see com.vaadin.data.Validator#validate(java.lang.Object)
         */
        public void validate(Object value) throws InvalidValueException {

            if (value == null) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_EMAIL_EMPTY_0));
            }
            try {
                CmsUser.checkEmail((String)value);
            } catch (CmsIllegalArgumentException e) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_EMAIL_INVALID_0));
            }
        }
    }

    /**
     * Validator for the login name field.<p>
     */
    class LoginNameValidator implements Validator {

        /**vaadin serial id.*/
        private static final long serialVersionUID = -6768717591898665618L;

        /**
         * @see com.vaadin.data.Validator#validate(java.lang.Object)
         */
        public void validate(Object value) throws InvalidValueException {

            if (value == null) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_LOGINNAME_EMPTY_0));
            }

            try {
                CmsDefaultValidationHandler handler = new CmsDefaultValidationHandler();
                handler.checkUserName((String)value);
            } catch (CmsIllegalArgumentException e) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_LOGINNAME_INVALID_0));
            }
        }
    }

    /**
     * Validator for password fields.<p>
     */
    class PasswordValidator implements Validator {

        /**vaadin serial id.*/
        private static final long serialVersionUID = 64216980175982548L;

        /**
         * @see com.vaadin.data.Validator#validate(java.lang.Object)
         */
        public void validate(Object value) throws InvalidValueException {

            if (isPasswordMismatchingConfirm()) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(
                        Messages.GUI_USERMANAGEMENT_USER_VALIDATION_PASSWORD_NOT_EQUAL_CONFIRM_0));
            }

            if (!isNewUser()) {
                if ((value == null) | CmsStringUtil.isEmptyOrWhitespaceOnly((String)value)) {
                    return; //ok, password was not changed for existing user
                }
            }

            if (!isPasswordValid()) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_PASSWORD_INVALID_0));
            }
        }
    }

    /**
     * Validator for start view and start site field.<p>
     */
    class StartViewValidator implements Validator {

        /**vaadin serial id.*/
        private static final long serialVersionUID = -4257155941690487831L;

        /**
         * @see com.vaadin.data.Validator#validate(java.lang.Object)
         */
        public void validate(Object value) throws InvalidValueException {

            if (isRootSiteSelected() & !isStartViewAvailableOnRoot()) {
                throw new InvalidValueException(
                    CmsVaadinUtils.getMessageText(Messages.GUI_USERMANAGEMENT_USER_VALIDATION_STARTVIEW_NOTFORROOT_0));
            }

        }
    }

    /**vaadin serial id.*/
    private static final long serialVersionUID = -5198443053070008413L;

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsUserEditDialog.class);

    /**vaadin component.*/
    private CmsUser m_user;

    /**CmsObject. */
    private CmsObject m_cms;

    /**vaadin component.*/
    private Button m_ok;

    /**vaadin component.*/
    private Button m_cancel;

    /**vaadin component.*/
    private TextField m_loginname;

    /**Password form. */
    private CmsPasswordForm m_pw;

    /**vaadin component.*/
    private ComboBox m_language;

    /**vaadin component.*/
    ComboBox m_site;

    /**vaadin component.*/
    private ComboBox m_project;

    /**vaadin component.*/
    CmsPathSelectField m_startfolder;

    /**User data form.<p>*/
    private CmsUserDataFormLayout m_userdata;

    /**vaadin component.*/
    private ComboBox m_startview;

    /**vaadin component.*/
    private CheckBox m_enabled;

    /**vaadin component.*/
    private CheckBox m_selfmanagement;

    /**vaadin component.*/
    private TabSheet m_tab;

    /**vaadin component.*/
    private Label m_ou;

    /**vaadin component.*/
    private TextArea m_description;

    /**Holder for authentification fields. */
    private VerticalLayout m_authHolder;

    /**Select view for principals.*/
    private CmsPrincipalSelect m_group;

    /**
     * public constructor.<p>
     *
     * @param cms CmsObject
     * @param userId id of user
     * @param window to be closed
     */
    public CmsUserEditDialog(CmsObject cms, CmsUUID userId, final Window window) {

        CmsVaadinUtils.readAndLocalizeDesign(this, CmsVaadinUtils.getWpMessagesForCurrentLocale(), null);
        setPasswordFields();
        try {
            m_cms = OpenCms.initCmsObject(cms);
            m_startfolder.disableSiteSwitch();
            m_user = m_cms.readUser(userId);
            displayResourceInfoDirectly(Collections.singletonList(CmsAccountsApp.getPrincipalInfo(m_user)));
            m_group.setVisible(false);
            m_loginname.setValue(m_user.getSimpleName());
            m_loginname.setEnabled(false);
            m_ou.setValue(m_user.getOuFqn());

            m_description.setValue(m_user.getDescription());
            m_selfmanagement.setValue(new Boolean(!m_user.isManaged()));
            m_enabled.setValue(new Boolean(m_user.isEnabled()));
            CmsUserSettings settings = new CmsUserSettings(m_user);
            init(window, settings);
            m_startfolder.setValue(settings.getStartFolder());
            m_startfolder.setCmsObject(getCmsObjectWithSite((String)m_site.getValue()));
            m_startfolder.setUseRootPaths(false);

            m_loginname.setEnabled(false);

        } catch (CmsException e) {
            LOG.error("Can't read user", e);
        }
    }

    /**
     * public constructor for new user case.<p>
     *
     * @param cms CmsObject
     * @param window Window
     * @param ou organizational unit
     */
    public CmsUserEditDialog(CmsObject cms, final Window window, String ou) {
        CmsVaadinUtils.readAndLocalizeDesign(this, CmsVaadinUtils.getWpMessagesForCurrentLocale(), null);
        try {
            m_cms = OpenCms.initCmsObject(cms);
        } catch (CmsException e) {
            //
        }
        setPasswordFields();

        m_ou.setValue(ou);
        m_group.setWidgetType(WidgetType.groupwidget);
        m_group.setValue(ou + OpenCms.getDefaultUsers().getGroupUsers());
        m_group.setRealPrincipalsOnly(true);
        m_enabled.setValue(Boolean.TRUE);
        init(window, null);
    }

    /**
     * Checks if a new user should be created.<p>
     *
     * @return true, if create user function
     */
    protected boolean isNewUser() {

        return m_user == null;
    }

    /**
     * Is password not matching to confirm field?<p>
     *
     * @return true, if password not equal to confirm
     */
    protected boolean isPasswordMismatchingConfirm() {

        return !m_pw.getPassword1().equals(m_pw.getPassword2());

    }

    /**
     * Validates the password fields.<p>
     *
     * @return true if password is valid (and confirm field matches password field).<p>
     */
    protected boolean isPasswordValid() {

        if ((m_pw.getPassword1() == null) | (m_pw.getPassword2() == null)) {
            return false;
        }
        try {
            CmsPasswordInfo pwdInfo = new CmsPasswordInfo();
            pwdInfo.setNewPwd(m_pw.getPassword1());
            pwdInfo.setConfirmation(m_pw.getPassword2());

            pwdInfo.validate();
            return true;
        } catch (CmsIllegalArgumentException | CmsIllegalStateException e) {
            LOG.error("New password is not ok", e);
            return false;
        }
    }

    /**
     * Checks if currently the root site is chosen as start site.<p>
     *
     * @return true if root site was selected
     */
    protected boolean isRootSiteSelected() {

        return m_site.getValue().equals("");
    }

    /**
     * Checks if the currently chosen start view is visible for root site.<p>
     *
     * @return true if app is available for root site
     */
    protected boolean isStartViewAvailableOnRoot() {

        if (!m_startview.isEnabled()) {
            return false;
        }

        return !m_startview.getValue().equals(CmsPageEditorConfiguration.APP_ID)
            & !m_startview.getValue().equals(CmsSitemapEditorConfiguration.APP_ID);
    }

    /**
     * Checks if all fields are valid. If not the tab of the first invalid field gets chosen.<p>
     *
     * @return true, if everything is ok
     */
    protected boolean isValid() {

        boolean[] ret = new boolean[4];
        ret[0] = m_loginname.isValid();
        ret[1] = m_userdata.isValid();
        ret[3] = m_site.isValid() & m_startview.isValid();
        ret[2] = m_pw.getPassword1Field().isValid();

        for (int i = 0; i < ret.length; i++) {

            if (!ret[i]) {
                m_tab.setSelectedTab(i);
                break;
            }
        }
        return ret[0] & ret[1] & ret[2] & ret[3];
    }

    /**
     * Saves the canged user data.<p>
     */
    protected void save() {

        try {
            if (m_user == null) {
                createNewUser();
            } else {
                saveUser();
            }

            saveUserSettings();
        } catch (CmsException e) {
            LOG.error("Unable to save user", e);
        }
    }

    /**
     * Sets up the validators.<p>
     */
    protected void setupValidators() {

        if (m_loginname.getValidators().size() == 0) {
            m_loginname.addValidator(new LoginNameValidator());
            m_pw.getPassword1Field().addValidator(new PasswordValidator());
            m_site.addValidator(new StartViewValidator());
            m_startview.addValidator(new StartViewValidator());
        }
    }

    /**
     * Checks whether the passwords match.<p>
     *
     * @param password2 the password 2 value
     */
    void checkPasswordMatch(String password2) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(password2)) {
            showPasswordMatchError(!password2.equals(m_pw.getPassword1()));
        }
    }

    /**
     * Checks the security level of the given password.<p>
     *
     * @param password the password
     */
    void checkSecurity(String password) {

        I_CmsPasswordHandler handler = OpenCms.getPasswordHandler();
        try {
            handler.validatePassword(password);
            if (handler instanceof I_CmsPasswordSecurityEvaluator) {
                SecurityLevel level = ((I_CmsPasswordSecurityEvaluator)handler).evaluatePasswordSecurity(password);
                m_pw.setErrorPassword1(null, OpenCmsTheme.SECURITY + "-" + level.name());
            } else {
                m_pw.setErrorPassword1(null, OpenCmsTheme.SECURITY_STRONG);
            }
        } catch (CmsSecurityException e) {
            m_pw.setErrorPassword1(
                new UserError(e.getLocalizedMessage(A_CmsUI.get().getLocale())),
                OpenCmsTheme.SECURITY_INVALID);
        }
        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_pw.getPassword2())) {
            showPasswordMatchError(!password.equals(m_pw.getPassword2()));
        }
    }

    /**
     * Returns a CmsObject with request set to given site.<p>
     *
     * @param siteRoot to be used
     * @return CmsObject
     */
    CmsObject getCmsObjectWithSite(String siteRoot) {

        if (siteRoot == null) {
            siteRoot = "/";
        }
        try {
            CmsObject res = OpenCms.initCmsObject(m_cms);
            res.getRequestContext().setSiteRoot(siteRoot);
            return res;
        } catch (CmsException e) {
            LOG.error("Unable to initialize CmsObject", e);
            return m_cms;
        }
    }

    /**
     * Initializes the site combo box.<p>
     *
     * @param settings user settings
     */
    void iniSite(CmsUserSettings settings) {

        List<CmsSite> sitesList = OpenCms.getSiteManager().getAvailableSites(m_cms, true, false, m_ou.getValue());

        IndexedContainer container = new IndexedContainer();
        container.addContainerProperty("caption", String.class, "");
        CmsSite firstNoRootSite = null;
        for (CmsSite site : sitesList) {
            if (CmsStringUtil.isEmptyOrWhitespaceOnly(site.getSiteRoot())) {
                if (hasRole(CmsRole.VFS_MANAGER)
                    | ((m_user == null)
                        & m_group.getValue().equals(OpenCms.getDefaultUsers().getGroupAdministrators()))) {
                    Item item = container.addItem(site.getSiteRoot());
                    System.out.println(site.getSiteRoot());
                    item.getItemProperty("caption").setValue(site.getTitle());
                }
            } else {
                if (firstNoRootSite == null) {
                    firstNoRootSite = site;
                }
                Item item = container.addItem(site.getSiteRoot());
                item.getItemProperty("caption").setValue(site.getTitle());
            }
        }
        m_site.setContainerDataSource(container);
        m_site.setItemCaptionPropertyId("caption");
        m_site.setNewItemsAllowed(false);
        m_site.setNullSelectionAllowed(false);
        if (settings != null) {
            if (settings.getStartSite().length() >= 1) {
                m_site.select(settings.getStartSite().substring(0, settings.getStartSite().length() - 1));
            } else {
                LOG.error("The start site is unvalid configured");
            }
        } else {
            if (firstNoRootSite != null) {
                m_site.select(firstNoRootSite.getSiteRoot());
            } else {
                m_site.select(m_site.getItemIds().iterator().next());
            }
        }
    }

    /**
     * Initializes the start view.<p>
     *
     * @param settings user settings
     */
    void iniStartView(CmsUserSettings settings) {

        IndexedContainer container = getStartViewContainer("caption");
        if (container.size() > 0) {
            m_startview.setEnabled(true);
            m_startview.setContainerDataSource(container);
            m_startview.setItemCaptionPropertyId("caption");
            m_startview.setNullSelectionAllowed(false);
            m_startview.setNewItemsAllowed(false);
            if (container.getItemIds().size() > 0) {
                m_startview.select(container.getItemIds().get(0));
                if (settings != null) {
                    m_startview.select(settings.getStartView());
                }
            }
        } else {
            m_startview.setEnabled(false);
        }
    }

    /**
     * Shows or hides the not matching passwords error.<p>
     *
     * @param show <code>true</code> to show the error
     */
    void showPasswordMatchError(boolean show) {

        if (show) {

            m_pw.setErrorPassword2(
                new UserError(CmsVaadinUtils.getMessageText(org.opencms.ui.Messages.GUI_PWCHANGE_PASSWORD_MISMATCH_0)),
                OpenCmsTheme.SECURITY_INVALID);
        } else {
            m_pw.setErrorPassword2(null, OpenCmsTheme.SECURITY_STRONG);
        }
    }

    /**
     * Creates new user.<p>
     *
     * @throws CmsException exception
     */
    private void createNewUser() throws CmsException {

        //Password was checked by validator before
        String ou = m_ou.getValue();
        if (!ou.endsWith("/")) {
            ou += "/";
        }
        CmsUser user = m_cms.createUser(ou + m_loginname.getValue(), m_pw.getPassword1(), "", null);
        updateUser(user);
        m_cms.writeUser(user);
        if (!CmsStringUtil.isEmptyOrWhitespaceOnly(m_group.getValue())) {
            m_cms.addUserToGroup(user.getName(), m_group.getValue());
        }
        m_user = user;

    }

    /**
     * Returns the start view container.<p>
     *
     * @param caption of the container
     * @return indexed container
     */
    private IndexedContainer getStartViewContainer(String caption) {

        List<I_CmsWorkplaceAppConfiguration> apps = OpenCms.getWorkplaceAppManager().getDefaultQuickLaunchConfigurations();

        IndexedContainer res = new IndexedContainer();

        res.addContainerProperty(caption, String.class, "");

        for (I_CmsWorkplaceAppConfiguration app : apps) {
            if (hasRoleForApp(app)) {
                Item item = res.addItem(app.getId());
                item.getItemProperty(caption).setValue(app.getName(A_CmsUI.get().getLocale()));
            }
        }
        return res;
    }

    /**
     * Checks if user, which gets edited, has given role.<p>
     *
     * @param role to be checked
     * @return true if user has role (or a parent role)
     */
    private boolean hasRole(CmsRole role) {

        if (m_user != null) {
            return OpenCms.getRoleManager().hasRole(m_cms, m_user.getName(), CmsRole.VFS_MANAGER);
        }
        return false;
    }

    /**
     * Checks if user, which gets edited, has role for given app.<p>
     *
     * @param app to be checked
     * @return true if user has role
     */
    private boolean hasRoleForApp(I_CmsWorkplaceAppConfiguration app) {

        if (m_user != null) {
            return OpenCms.getRoleManager().hasRole(m_cms, m_user.getName(), app.getRequiredRole());
        }

        if (!CmsStringUtil.isEmptyOrWhitespaceOnly(m_group.getValue())) {
            try {
                CmsGroup group = m_cms.readGroup(m_group.getValue());
                CmsRole roleFromGroup = CmsRole.valueOf(group);
                if (roleFromGroup == null) {
                    return false;
                }
                List<CmsRole> groupRoles = roleFromGroup.getChildren(true);
                groupRoles.add(CmsRole.valueOf(group));
                List<String> roleNames = new ArrayList<String>();
                for (CmsRole gr : groupRoles) {
                    roleNames.add(gr.getRoleName());
                }
                return roleNames.contains(app.getRequiredRole().getRoleName());
            } catch (CmsException e) {
                LOG.error("Unable to read group", e);
            }
        }
        return true;
    }

    /**
     * Initializes the language combo box.<p>
     *
     * @param settings user settings
     */
    private void iniLanguage(CmsUserSettings settings) {

        m_language.setContainerDataSource(CmsVaadinUtils.getLanguageContainer("caption"));
        m_language.setItemCaptionPropertyId("caption");
        m_language.setNewItemsAllowed(false);
        m_language.setNullSelectionAllowed(false);
        if (settings != null) {
            m_language.select(settings.getLocale());
        } else {
            m_language.select(m_language.getItemIds().iterator().next());
        }
    }

    /**
     * Initializes the project combo box.<p>
     *
     * @param settings of user
     */
    private void iniProject(CmsUserSettings settings) {

        try {
            List<CmsProject> projects = OpenCms.getOrgUnitManager().getAllAccessibleProjects(
                m_cms,
                m_ou.getValue(),
                false);
            for (CmsProject project : projects) {
                m_project.addItem(project.getSimpleName());
            }
            m_project.setNewItemsAllowed(false);
            m_project.setNullSelectionAllowed(false);
            m_project.select(CmsProject.ONLINE_PROJECT_NAME);
            if (settings != null) {
                String projString = settings.getStartProject();
                if (projString.contains("/")) {
                    projString = projString.split("/")[projString.split("/").length - 1];
                }
                m_project.select(projString);
            } else {
                m_project.select(m_project.getItemIds().iterator().next());
            }
        } catch (CmsException e) {
            LOG.error("Unable to read projects", e);
        }
    }

    /**
     * A initialization method.<p>
     *
     * @param window to be closed
     * @param settings user settings, null if new user
     */
    private void init(final Window window, final CmsUserSettings settings) {

        m_userdata.initFields(m_user, true);
        iniLanguage(settings);
        iniProject(settings);
        iniSite(settings);
        iniStartView(settings);

        m_ok.addClickListener(new ClickListener() {

            private static final long serialVersionUID = -2579639520410382246L;

            public void buttonClick(ClickEvent event) {

                setupValidators();
                if (isValid()) {
                    save();
                    window.close();
                }
                A_CmsUI.get().reload();
            }
        });

        m_cancel.addClickListener(new ClickListener() {

            private static final long serialVersionUID = 5803825104722705175L;

            public void buttonClick(ClickEvent event) {

                window.close();
            }
        });

        if (m_user == null) {
            m_group.addValueChangeListener(new ValueChangeListener() {

                private static final long serialVersionUID = 1512940002751242094L;

                public void valueChange(ValueChangeEvent event) {

                    iniStartView(settings);
                    iniSite(settings);
                }

            });
        }

        m_site.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = -169973382455098800L;

            public void valueChange(ValueChangeEvent event) {

                m_startfolder.setCmsObject(getCmsObjectWithSite((String)m_site.getValue()));

            }

        });
    }

    /**
     * Saves changes to an existing user.<p>
     *
     * @throws CmsException exception
     */
    private void saveUser() throws CmsException {

        updateUser(m_user);

        if (!CmsStringUtil.isEmptyOrWhitespaceOnly(m_pw.getPassword1())) {
            if (isPasswordValid()) {
                m_cms.setPassword(m_user.getName(), m_pw.getPassword1());
            }
        }

        m_cms.writeUser(m_user);

    }

    /**
     * Saves the user settings.<p>
     *
     * @throws CmsException exception
     */
    private void saveUserSettings() throws CmsException {

        CmsUserSettings settings = new CmsUserSettings(m_user);
        settings.setLocale((Locale)m_language.getValue());
        settings.setStartSite((String)m_site.getValue() + "/");
        settings.setStartProject(m_ou.getValue() + (String)m_project.getValue());
        settings.setStartFolder(m_startfolder.getValue());
        settings.setStartView((String)m_startview.getValue());
        settings.save(m_cms);
    }

    /**
     * Sets the password fields.<p>
     */
    private void setPasswordFields() {

        m_pw.hideOldPassword();
        m_pw.setHeaderVisible(false);
        if (OpenCms.getPasswordHandler() instanceof I_CmsPasswordSecurityEvaluator) {
            m_pw.setSecurityHint(
                ((I_CmsPasswordSecurityEvaluator)OpenCms.getPasswordHandler()).getPasswordSecurityHint(
                    A_CmsUI.get().getLocale()));
        }
        m_pw.getOldPasswordField().setImmediate(true);
        m_pw.getPassword1Field().setImmediate(true);
        m_pw.getPassword2Field().setImmediate(true);

        m_pw.getPassword1Field().addTextChangeListener(new TextChangeListener() {

            private static final long serialVersionUID = 1L;

            public void textChange(TextChangeEvent event) {

                checkSecurity(event.getText());
            }
        });
        m_pw.getPassword2Field().addTextChangeListener(new TextChangeListener() {

            private static final long serialVersionUID = 1L;

            public void textChange(TextChangeEvent event) {

                checkPasswordMatch(event.getText());
            }
        });

        m_authHolder.addComponent(m_pw);
    }

    /**
     *  Read form and updates a given user according to form.<p>
     *
     * @param user to be updated
     */
    private void updateUser(CmsUser user) {

        user.setDescription(m_description.getValue());
        user.setManaged(!m_selfmanagement.getValue().booleanValue());
        m_userdata.submit(user, m_cms, new Runnable() {

            public void run() {
                //
            }
        });
    }
}