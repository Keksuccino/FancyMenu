package de.keksuccino.fancymenu.rendering.ui.widget;

import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FeedbackButton extends Button {

    protected static final Logger LOGGER = LogManager.getLogger();

    protected FeedbackClickAction feedbackClickAction;
    protected List<Consumer<Boolean>> feedbackListeners = new ArrayList<>();

    public FeedbackButton(int x, int y, int widthIn, int heightIn, @NotNull Component label, boolean handleSelf, @NotNull FeedbackClickAction feedbackClickAction) {
        super(x, y, widthIn, heightIn, label, handleSelf, (p) -> {});
        this.feedbackClickAction = feedbackClickAction;
    }

    public void addFeedbackListener(@NotNull Consumer<Boolean> listener) {
        this.feedbackListeners.add(listener);
    }

    @Override
    public void onPress() {
        this.feedbackClickAction.onClick(this, (b) -> {
            for (Consumer<Boolean> c : this.feedbackListeners) {
                c.accept(b);
            }
        });
    }

    @Override
    public void setPressAction(OnPress press) {
        LOGGER.error("Press action can't be changed for FeedbackButtons!");
        new Throwable().printStackTrace();
    }

    @FunctionalInterface
    public interface FeedbackClickAction {

        void onClick(FeedbackButton button, Consumer<Boolean> feedback);

    }

}
