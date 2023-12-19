/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.installer.util;

import java.awt.event.MouseEvent;

import javax.swing.text.DefaultCaret;

// Disables text selection and the caret.
public class NoopCaret extends DefaultCaret {
	public NoopCaret() {
		setBlinkRate(0);
		super.setSelectionVisible(false);
	}

	@Override
	public int getDot() {
		return 0;
	}

	@Override
	public int getMark() {
		return 0;
	}

	@Override
	public void setSelectionVisible(boolean vis) {
	}

	@Override
	protected void positionCaret(MouseEvent e) {
	}

	@Override
	protected void moveCaret(MouseEvent e) {
	}
}
