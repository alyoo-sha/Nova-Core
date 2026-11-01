package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.graphics.UiText;

public record DialogButton(UiText label, Runnable action) {}