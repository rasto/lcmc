/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Jsr330ScopeMetadataResolver;

public final class AppContext {
	private static AnnotationConfigApplicationContext context;

	static {
		final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.setScopeMetadataResolver(new Jsr330ScopeMetadataResolver());
        ctx.scan("lcmc");
        ctx.refresh();
        context = ctx;
	}

	private AppContext() {
	}

	public static <T> T getBean(Class<T> beanClass) {
		return context.getBean(beanClass);
	}

    public static <T> T getBean(final String name, Class<T> beanClass) {
        return context.getBean(name, beanClass);
    }
}
