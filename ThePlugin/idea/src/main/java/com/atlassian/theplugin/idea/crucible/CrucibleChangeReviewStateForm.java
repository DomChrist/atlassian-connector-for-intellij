/**
 * Copyright (C) 2008 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.theplugin.idea.crucible;

import com.atlassian.theplugin.commons.crucible.CrucibleServerFacade;
import com.atlassian.theplugin.commons.crucible.CrucibleServerFacadeImpl;
import com.atlassian.theplugin.commons.crucible.api.model.Action;
import com.atlassian.theplugin.commons.crucible.api.model.Review;
import com.atlassian.theplugin.commons.exception.ServerPasswordNotProvidedException;
import com.atlassian.theplugin.commons.remoteapi.RemoteApiException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import static com.intellij.openapi.ui.Messages.showMessageDialog;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class CrucibleChangeReviewStateForm extends DialogWrapper {
    private JPanel rootComponent;
    private JTextArea summaryArea;
    private JScrollPane summaryScrollPane;
    private JLabel idLabel;

    private ReviewData review;
    private CrucibleServerFacade crucibleServerFacade;
    private Action action;

    protected CrucibleChangeReviewStateForm(ReviewData review, Action action) {
        super(false);
        this.review = review;
        this.action = action;
        this.crucibleServerFacade = CrucibleServerFacadeImpl.getInstance();

        $$$setupUI$$$();
        init();

        if (action.equals(com.atlassian.theplugin.commons.crucible.api.model.Action.CLOSE)) {
            summaryScrollPane.setVisible(true);
            setTitle("Close review");
            getOKAction().putValue(javax.swing.Action.NAME, "Close review...");
        } else {
            summaryScrollPane.setVisible(false);
            switch (action) {
                case APPROVE:
                    setTitle("Approve review");
                    getOKAction().putValue(javax.swing.Action.NAME, "Approve review...");
                    break;
                case ABANDON:
                    setTitle("Abandon review");
                    getOKAction().putValue(javax.swing.Action.NAME, "Abandon review...");
                    break;
                case SUMMARIZE:
                    setTitle("Summarize review");
                    getOKAction().putValue(javax.swing.Action.NAME, "Summarize review...");
                    break;
                case REOPEN:
                    setTitle("Reopen review");
                    getOKAction().putValue(javax.swing.Action.NAME, "Reopen review...");
                    break;
                case RECOVER:
                    setTitle("Recover abandoned review");
                    getOKAction().putValue(javax.swing.Action.NAME, "Recover abandoned review...");
                    break;
            }
        }

        fillReviewInfo(review);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootComponent = new JPanel();
        rootComponent.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootComponent.setMinimumSize(new Dimension(450, 200));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(1, 1, 1, 1), -1, -1));
        rootComponent.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Server:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Repository:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootComponent.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootComponent;
    }

    private void fillReviewInfo(final ReviewData review) {
        getOKAction().setEnabled(false);

        new Thread(new Runnable() {
            public void run() {
                Review reviewInfo = null;
                try {
                    reviewInfo = crucibleServerFacade.getReview(review.getServer(), review.getPermId());
                } catch (RemoteApiException e) {
                    // nothing can be done here
                } catch (ServerPasswordNotProvidedException e) {
                    // nothing can be done here
                }
                final ReviewData finalReviewInfo = new ReviewDataImpl(reviewInfo, review.getServer());
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        updateReviewInfo(finalReviewInfo);
                    }
                });
            }
        }, "atlassian-idea-plugin crucible patch upload combos refresh").start();
    }

    private void updateReviewInfo(ReviewData reviewInfo) {
        String userName = reviewInfo.getServer().getUserName();
        this.idLabel.setText(reviewInfo.getPermId().getId() + ": " + reviewInfo.getName());
        getOKAction().setEnabled(true);
    }

    public JComponent getRootComponent
            () {
        return rootComponent;
    }

    @Nullable
    protected JComponent createCenterPanel() {
        return getRootComponent();
    }


    protected void doOKAction() {
        try {
            switch (action) {
                case APPROVE:
                    crucibleServerFacade.approveReview(review.getServer(), review.getPermId());
                    break;
                case ABANDON:
                    crucibleServerFacade.abandonReview(review.getServer(), review.getPermId());
                    break;
                case SUMMARIZE:
                    crucibleServerFacade.summarizeReview(review.getServer(), review.getPermId());
                    break;
                case CLOSE:
                    crucibleServerFacade.closeReview(review.getServer(), review.getPermId(), summaryArea.getText());
                    break;
                case REOPEN:
                    crucibleServerFacade.reopenReview(review.getServer(), review.getPermId());
                    break;
                case RECOVER:
                    crucibleServerFacade.recoverReview(review.getServer(), review.getPermId());
                    break;
            }

        } catch (RemoteApiException e) {
            showMessageDialog(e.getMessage(),
                    "Error changing review state: " + review.getServer().getUrlString(), Messages.getErrorIcon());
        } catch (ServerPasswordNotProvidedException e) {
            showMessageDialog(e.getMessage(), "Error changing review state: " + review.getServer().getUrlString(), Messages.getErrorIcon());
        }
        super.doOKAction();
    }

    private void createUIComponents
            () {
    }
}
