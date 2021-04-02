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
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.YggdrasilLoginService;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * The login dialog.
 */
public class LoginDialog extends JDialog {

    private final Launcher launcher;
    @Getter public Session session;
    @Getter public Boolean force_update;

    private final JTextField usernameText = new JTextField();
    private final JTextField idCombo = new JTextField();
    private final JPasswordField passwordText = new JPasswordField();
    private final JButton loginButton = new JButton(SharedLocale.tr("login.login"));
    private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));
    private final FormPanel formPanel = new FormPanel();
    private final JButton offlineButton = new JButton(SharedLocale.tr("accounts.add"));
    private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);

    /**
     * Create a new login dialog.
     *
     * @param owner the owner
     * @param launcher the launcher
     */
    public LoginDialog(Window owner, @NonNull Launcher launcher) {
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
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        usernameText.setEditable(true);

        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD));

        formPanel.addRow(new JLabel(SharedLocale.tr("login.name")), idCombo);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(26, 13, 13, 13));

        buttonsPanel.addGlue();
        //buttonsPanel.addElement(off_update);
        buttonsPanel.addElement(offlineButton);
        buttonsPanel.addElement(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(offlineButton);

        //passwordText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        //recoverButton.addActionListener(
        //        ActionListeners.openURL(recoverButton, launcher.getProperties().getProperty("resetPasswordUrl")));

        offlineButton.addActionListener(e -> prepareLogin());
        cancelButton.addActionListener(e -> dispose());
    }

    public void prepareLogin() {
        final String name = idCombo.getText();

           new OfflineSession(name);
        String playerName = idCombo.getText();
        if (idCombo.getSelectedText() != null) {
            playerName = idCombo.getSelectedText().toString();
        }
        if (playerName.contains("@")) {
            playerName = launcher.getProperties().getProperty("" +
                    "");
        }
        setResult(new OfflineSession(playerName));
    }

    private void attemptLogin(String username) {
        LoginCallable callable = new LoginCallable(username);
        ObservableFuture<Session> future = new ObservableFuture<Session>(
                launcher.getExecutor().submit(callable), callable);

        Futures.addCallback(future, new FutureCallback<Session>() {
            @Override
            public void onSuccess(Session result) {
                setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("login.loggingInTitle"), SharedLocale.tr("login.loggingInStatus"));
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void setResult(Session session) {
        this.session = session;
        dispose();
    }

    public static Session showLoginRequest(Window owner, Launcher launcher) {
        LoginDialog dialog = new LoginDialog(owner, launcher);
        dialog.setVisible(true);
        return dialog.getSession();
    }

    @RequiredArgsConstructor
    private class LoginCallable implements Callable<Session>, ProgressObservable {
        private final String username;

        @Override
        public Session call() throws AuthenticationException, IOException, InterruptedException {
            YggdrasilLoginService service = launcher.getYggdrasil();
            Session identity = service.login(username);

                Configuration config = launcher.getConfig();
                    config.setOfflineEnabled(true);
                    Persistence.commitAndForget(config);

                return identity;
        }

        @Override
        public double getProgress() {
            return -1;
        }

        @Override
        public String getStatus() {
            return SharedLocale.tr("login.loggingInStatus");
        }
    }

}
