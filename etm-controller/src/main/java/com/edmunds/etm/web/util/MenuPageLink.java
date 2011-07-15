/*
 * Copyright 2011 Edmunds.com, Inc.
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
package com.edmunds.etm.web.util;

import org.apache.click.Context;
import org.apache.click.Page;
import org.apache.click.control.PageLink;

/**
 * @author Ryan Holmes
 */
public class MenuPageLink extends PageLink{

    public MenuPageLink(String name) {
        super(name);
    }

    public MenuPageLink(String name, Class<? extends Page> targetPage) {
        super(name, targetPage);
    }

    public MenuPageLink(String name, String label, Class<? extends Page> targetPage) {
        super(name, label, targetPage);
    }

    public MenuPageLink(Class<? extends Page> targetPage) {
        super(targetPage);
    }

    public MenuPageLink() {
        super();
    }

    @Override
    public void onRender() {
        if (isSelected()) {
            addStyleClass("selected");
        }
    }

    private boolean isSelected() {

        Context context = getContext();
        String pagePath = context.getPagePath(getPageClass());
        String currentPath = context.getResourcePath();

        return pagePath != null && pagePath.equals(currentPath);
    }
}
