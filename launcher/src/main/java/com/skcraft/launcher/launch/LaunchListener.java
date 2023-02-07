/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.launch;

public interface LaunchListener {

    void instancesUpdated();

    void gameStarted();

    void gameClosed();

}
