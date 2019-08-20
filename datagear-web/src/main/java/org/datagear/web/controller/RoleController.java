/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.management.domain.Role;
import org.datagear.management.service.RoleService;
import org.datagear.management.service.RoleUserService;
import org.datagear.persistence.PagingQuery;
import org.datagear.util.IDUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.convert.ClassDataConverter;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 角色管理控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController
{
	@Autowired
	private RoleService roleService;

	@Autowired
	private RoleUserService roleUserService;

	public RoleController()
	{
		super();
	}

	public RoleController(MessageSource messageSource, ClassDataConverter classDataConverter, RoleService roleService,
			RoleUserService roleUserService)
	{
		super(messageSource, classDataConverter);
		this.roleService = roleService;
		this.roleUserService = roleUserService;
	}

	public RoleService getRoleService()
	{
		return roleService;
	}

	public void setRoleService(RoleService roleService)
	{
		this.roleService = roleService;
	}

	public RoleUserService getRoleUserService()
	{
		return roleUserService;
	}

	public void setRoleUserService(RoleUserService roleUserService)
	{
		this.roleUserService = roleUserService;
	}

	@RequestMapping("/add")
	public String add(HttpServletRequest request, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "role.addRole");
		model.addAttribute(KEY_FORM_ACTION, "saveAdd");

		return "/role/role_form";
	}

	@RequestMapping(value = "/saveAdd", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAdd(HttpServletRequest request, HttpServletResponse response, Role role)
	{
		if (isBlank(role.getName()))
			throw new IllegalInputException();

		role.setId(IDUtil.uuid());

		this.roleService.add(role);

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		Role role = this.roleService.getById(id);

		model.addAttribute("role", role);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "role.editRole");
		model.addAttribute(KEY_FORM_ACTION, "saveEdit");

		return "/role/role_form";
	}

	@RequestMapping(value = "/saveEdit", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEdit(HttpServletRequest request, HttpServletResponse response,
			Role role)
	{
		if (isBlank(role.getName()))
			throw new IllegalInputException();

		this.roleService.update(WebUtils.getUser(request, response), role);

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/view")
	public String view(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		Role role = this.roleService.getById(id);

		if (role == null)
			throw new RecordNotFoundException();

		model.addAttribute("role", role);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "role.viewRole");
		model.addAttribute(KEY_READONLY, true);

		return "/role/role_form";
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("id") String[] ids)
	{
		for (String id : ids)
			this.roleService.deleteById(WebUtils.getUser(request, response), id);

		return buildOperationMessageDeleteSuccessResponseEntity(request);
	}

	@RequestMapping(value = "/query")
	public String query(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "role.manageRole");

		return "/role/role_grid";
	}

	@RequestMapping(value = "/select")
	public String select(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "role.selectRole");
		model.addAttribute(KEY_SELECTONLY, "true");

		return "/role/role_grid";
	}

	@RequestMapping(value = "/queryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public List<Role> queryData(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		PagingQuery pagingQuery = getPagingQuery(request, null);

		List<Role> roles = this.roleService.query(WebUtils.getUser(request, response), pagingQuery);

		return roles;
	}

	@Override
	protected String buildMessageCode(String code)
	{
		return buildMessageCode("user", code);
	}
}
