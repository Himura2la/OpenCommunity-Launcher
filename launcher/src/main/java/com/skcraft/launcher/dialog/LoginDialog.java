/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.Configuration;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.AuthenticationException;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.auth.YggdrasilLoginService;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.FormPanel;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TextFieldPopupMenu;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The login dialog.
 */
public class LoginDialog extends JDialog {

    private final Launcher launcher;
    private final JLabel message = new JLabel(SharedLocale.tr("login.defaultMessage"));
    private final JTextField usernameText = new JTextField();
    private final JPasswordField passwordText = new JPasswordField();
    private final JButton loginButton = new JButton(SharedLocale.tr("login.login"));
    //private final LinkButton recoverButton = new LinkButton(SharedLocale.tr("login.recoverAccount"));
    private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));
    private final FormPanel formPanel = new FormPanel();
    private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
    @Getter
    private Session session;

    /**
     * Create a new login dialog.
     *
     * @param owner    the owner
     * @param launcher the launcher
     */
    public LoginDialog(Window owner, @NonNull Launcher launcher, Optional<ReloginDetails> reloginDetails) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.launcher = launcher;

        setTitle(SharedLocale.tr("login.title"));
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
            }
        });

        reloginDetails.ifPresent(details -> message.setText(details.message));
    }

    public static Session showLoginRequest(Window owner, Launcher launcher) {
        return showLoginRequest(owner, launcher, null);
    }

    public static Session showLoginRequest(Window owner, Launcher launcher, ReloginDetails reloginDetails) {
        LoginDialog dialog = new LoginDialog(owner, launcher, Optional.ofNullable(reloginDetails));
        dialog.setVisible(true);
        return dialog.getSession();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        usernameText.setEditable(true);

        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD));

        formPanel.addRow(message);
        formPanel.addRow(new JLabel(SharedLocale.tr("login.nickname")), usernameText);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(26, 13, 13, 13));

        //buttonsPanel.addElement(recoverButton);
        buttonsPanel.addGlue();
        buttonsPanel.addElement(loginButton);
        buttonsPanel.addElement(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);

        passwordText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        /*recoverButton.addActionListener(
                ActionListeners.openURL(recoverButton, launcher.getProperties().getProperty("resetPasswordUrl")));*/

        loginButton.addActionListener(e -> prepareLogin());
        cancelButton.addActionListener(e -> dispose());
    }

    @SuppressWarnings("deprecation")
    private void prepareLogin() {
        if (!usernameText.getText().isEmpty()) {
            setResult(new OfflineSession(usernameText.getText()));
        } else {
            SwingHelper.showErrorDialog(this, SharedLocale.tr("login.noLoginError"), SharedLocale.tr("login.noLoginTitle"));
        }
    }

    private void setResult(Session session) {
        this.session = session;
        dispose();
    }

    @Data
    public static class ReloginDetails {
        private final String username;
        private final String message;
    }
}