/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.util;

import java.util.Calendar;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.datagear.management.domain.User;
import org.datagear.util.IDUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.security.AuthUser;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Web工具集。
 * 
 * @author datagear@163.com
 *
 */
public class WebUtils
{
	public static final String SESSION_KEY_USER_ANONYMOUS = "USER_ANONYMOUS";

	public static final String COOKIE_USER_ID_ANONYMOUS = "USER_ID_ANONYMOUS";

	public static final String COOKIE_PAGINATION_SIZE = "PAGINATION_PAGE_SIZE";

	/** Servlet环境中存储操作消息的关键字 */
	public static final String KEY_OPERATION_MESSAGE = "operationMessage";

	/** 页面ID关键字 */
	public static final String KEY_PAGE_ID = "pageId";

	/** 父页面ID关键字 */
	public static final String KEY_PARENT_PAGE_ID = "parentPageId";

	/**
	 * 获取当前用户（认证用户或者匿名用户）。
	 * <p>
	 * 此方法不会返回{@code null}。
	 * </p>
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static User getUser(HttpServletRequest request, HttpServletResponse response)
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null)
		{
			User user = getUser(authentication);

			if (user != null)
				return user;
		}

		HttpSession session = request.getSession();

		User anonymousUser = (User) request.getSession().getAttribute(SESSION_KEY_USER_ANONYMOUS);

		if (anonymousUser == null)
		{
			String anonymousUserId = getCookieValue(request, COOKIE_USER_ID_ANONYMOUS);

			if (anonymousUserId == null || anonymousUserId.isEmpty())
			{
				anonymousUserId = IDUtil.uuid();

				Cookie cookie = new Cookie(COOKIE_USER_ID_ANONYMOUS, anonymousUserId);
				cookie.setPath(request.getContextPath() + "/");
				cookie.setMaxAge(60 * 60 * 24 * 365 * 10);

				response.addCookie(cookie);
			}

			anonymousUser = new User(anonymousUserId);
			anonymousUser.setName(anonymousUserId);
			anonymousUser.setAdmin(false);
			anonymousUser.setAnonymous(true);
			anonymousUser.setCreateTime(new java.util.Date());

			session.setAttribute(SESSION_KEY_USER_ANONYMOUS, anonymousUser);
		}

		return anonymousUser;
	}

	/**
	 * 获取认证用户。
	 * <p>
	 * 如果未认证，此方法将返回{@code null}。
	 * </p>
	 * 
	 * @param authentication
	 * @return
	 */
	public static User getUser(Authentication authentication)
	{
		Object principal = authentication.getPrincipal();

		if (principal instanceof User)
		{
			return (User) principal;
		}
		else if (principal instanceof AuthUser)
		{
			AuthUser ou = (AuthUser) principal;
			return ou.getUser();
		}
		else
			return null;
	}

	/**
	 * 获取操作消息。
	 * 
	 * @param request
	 * @return
	 */
	public static OperationMessage getOperationMessage(HttpServletRequest request)
	{
		return (OperationMessage) request.getAttribute(KEY_OPERATION_MESSAGE);
	}

	/**
	 * 设置异常操作消息。
	 * 
	 * @param request
	 * @param operationMessage
	 * @return
	 */
	public static void setOperationMessage(HttpServletRequest request, OperationMessage operationMessage)
	{
		request.setAttribute(KEY_OPERATION_MESSAGE, operationMessage);
	}

	/**
	 * 获取{@linkplain Cookie}。
	 * 
	 * @param request
	 * @param cookieName
	 * @return
	 */
	public static String getCookieValue(HttpServletRequest request, String cookieName)
	{
		Cookie[] cookies = request.getCookies();

		if (cookies == null)
			return null;

		for (Cookie cookie : cookies)
		{
			if (cookieName.equals(cookie.getName()))
				return cookie.getValue();
		}

		return null;
	}

	/**
	 * 获取请求{@linkplain Locale}。
	 * 
	 * @param request
	 * @return
	 */
	public static Locale getLocale(HttpServletRequest request)
	{
		return LocaleContextHolder.getLocale();
	}

	/**
	 * 获取当前主题。
	 * 
	 * @param request
	 * @return
	 * @throws IllegalStateException
	 */
	public static String getTheme(HttpServletRequest request) throws IllegalStateException
	{
		return getThemeResolver(request).resolveThemeName(request);
	}

	/**
	 * 获取当前{@linkplain ThemeResolver}，没有则抛出{@linkplain IllegalStateException}异常。
	 * 
	 * @param request
	 * @return
	 * @throws IllegalStateException
	 */
	public static ThemeResolver getThemeResolver(HttpServletRequest request) throws IllegalStateException
	{
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(request);

		if (themeResolver == null)
			throw new IllegalStateException("No ThemeResolver found: not in a DispatcherServlet request?");

		return themeResolver;
	}

	/**
	 * 以秒数滚动时间。
	 * 
	 * @param date
	 * @param seconds
	 * @return
	 */
	public static java.util.Date addSeconds(java.util.Date date, int seconds)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		calendar.add(Calendar.SECOND, seconds);

		return calendar.getTime();
	}

	/**
	 * 判断是否是JSON响应。
	 * 
	 * @param response
	 * @return
	 */
	public static boolean isJsonResponse(HttpServletResponse response)
	{
		String __contentType = response.getContentType();
		if (__contentType == null)
			__contentType = "";
		else
			__contentType = __contentType.toLowerCase();

		return (__contentType.indexOf("json") >= 0);
	}

	/**
	 * 判断请求是否是ajax请求。
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isAjaxRequest(HttpServletRequest request)
	{
		// 是否ajax请求，jquery库ajax可以使用此方案判断
		boolean ajaxRequest = (request.getHeader("x-requested-with") != null);

		return ajaxRequest;
	}

	/**
	 * 获取页面ID。
	 * <p>
	 * 页面ID作为一次请求的客户端标识，用于为客户端定义页面对象。
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	public static String getPageId(HttpServletRequest request)
	{
		return (String) request.getAttribute(KEY_PAGE_ID);
	}

	/**
	 * 设置页面ID。
	 * <p>
	 * 设置后，在页面可以使用EL表达式<code>${pageId}</code>来获取。
	 * </p>
	 * 
	 * @param request
	 * @param pageId
	 */
	public static void setPageId(HttpServletRequest request, String pageId)
	{
		request.setAttribute(KEY_PAGE_ID, pageId);
	}

	/**
	 * 设置页面ID。
	 * 
	 * @param request
	 * @return
	 */
	public static String setPageId(HttpServletRequest request)
	{
		String pageId = generatePageId();
		setPageId(request, pageId);

		return pageId;
	}

	/**
	 * 生成页面ID。
	 * 
	 * @return
	 */
	public static String generatePageId()
	{
		return generatePageId("p");
	}

	/**
	 * 生成页面ID。
	 * 
	 * @param prefix
	 * @return
	 */
	public static String generatePageId(String prefix)
	{
		long timeMs = System.currentTimeMillis();
		long random = Math.round(Math.random() * 100000);
		return prefix + Long.toHexString(timeMs) + Long.toHexString(random);
	}

	/**
	 * 获取父页面ID。
	 * <p>
	 * 如果没有定义，则返回空字符串。
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	public static String getParentPageId(HttpServletRequest request)
	{
		String parentPage = request.getParameter(KEY_PARENT_PAGE_ID);

		if (parentPage == null)
			parentPage = (String) request.getAttribute(KEY_PARENT_PAGE_ID);

		if (parentPage == null)
			parentPage = "";

		return parentPage;
	}

	/**
	 * 获取页面表单ID。
	 * 
	 * @param request
	 * @return
	 */
	public static String getPageFormId(HttpServletRequest request)
	{
		return getPageElementId(request, "form");
	}

	/**
	 * 获取页面表格ID。
	 * 
	 * @param request
	 * @return
	 */
	public static String getPageTableId(HttpServletRequest request)
	{
		return getPageElementId(request, "table");
	}

	/**
	 * 获取页面元素ID。
	 * 
	 * @param request
	 * @param elementId
	 * @return
	 */
	public static String getPageElementId(HttpServletRequest request, String elementId)
	{
		String pageId = getPageId(request);
		return pageId + "-" + elementId;
	}

	/**
	 * 从请求获取布尔值。
	 * 
	 * @param request
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBooleanValue(HttpServletRequest request, String name, boolean defaultValue)
	{
		Object value = request.getAttribute(name);

		if (value != null)
		{
			if (value instanceof Boolean)
				return (Boolean) value;

			return ("true".equals(value.toString()) || "1".equals(value.toString()));
		}

		String paramValue = request.getParameter(name);

		if (paramValue != null)
			return ("true".equals(paramValue) || "1".equals(paramValue));

		return defaultValue;
	}

	/**
	 * 从请求获取字符串值。
	 * 
	 * @param request
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getStringValue(HttpServletRequest request, String name, String defaultValue)
	{
		Object value = request.getAttribute(name);

		if (value != null)
		{
			if (value instanceof String)
				return (String) value;

			return value.toString();
		}

		String paramValue = request.getParameter(name);

		if (paramValue != null)
			return paramValue;

		return defaultValue;
	}
}
