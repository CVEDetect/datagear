<#--
 *
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 *
-->
<#include "../include/page_import.ftl">
<#include "../include/html_doctype.ftl">
<html>
<head>
<#include "../include/html_head.ftl">
<title>
	<#include "../include/html_app_name_prefix.ftl">
	<@spring.message code='module.schema' />
	<#include "../include/html_request_action_suffix.ftl">
</title>
</head>
<body class="p-card no-border">
<#include "../include/page_obj.ftl">
<div id="${pid}" class="page page-query page-query-schema">
	<div class="grid grid-nogutter m-0 flex-nowrap">
		<div class="col-3">
			<div class="flex flex-column m-0">
				<div class="page-header">
					<div class="grid align-items-center flex-nowrap">
						<div class="col">
							<form @submit.prevent="onSearch" class="py-1">
								<div class="p-inputgroup">
									<p-button type="button" :icon="pm.searchType=='schema' ? 'pi pi-database' : 'pi pi-table'"
										class="p-button-secondary" @click="onToggleSearchType(e)"
										title="<@spring.message code='switchSearchMode' />">
									</p-button>
									<p-inputtext type="text" v-model="pm.searchForm.keyword"></p-inputtext>
									<p-button type="submit" icon="pi pi-search"></p-button>
								</div>
							</form>
						</div>
						<div class="col-fixed text-right">
							<p-splitbutton icon="pi pi-plus" @click="onAdd" :model="pm.schemaOptItems"></p-splitbutton>
						</div>
					</div>
				</div>
				<div class="page-content flex-grow-1 p-0">
					<p-tree :value="pm.schemaNodes"
						selection-mode="multiple" v-model:selection-keys="pm.selectedNodeKeys"
						@node-expand="onSchemaNodeExpand" @node-select="onSchemaNodeSelect"
						:loading="pm.loadingSchema" class="schema-tree h-full overflow-auto">
					</p-tree>
				</div>
			</div>
		</div>
		<div class="table-tabs-wrapper col-9 pl-3">
			<p-tabview v-model:active-index="pm.tableTabs.activeIndex" :scrollable="true" @tab-change="onTableTabChange" @tab-click="onTableTabClick" class="contextmenu-tabview">
				<p-tabpanel v-for="tab in pm.tableTabs.items" :key="tab.id" :header="tab.title" :id="tab.id">
					<template #header>
						<p-button type="button" icon="pi pi-angle-down" class="context-menu-btn p-button-secondary p-button-text p-button-rounded"
							@click="onTableTabMenuToggle" aria-haspopup="true" aria-controls="${pid}-tableTabMenu">
						</p-button>
					</template>
					<div :id="tab.id"></div>
				</p-tabpanel>
			</p-tabview>
			<p-contextmenu id="${pid}-tableTabMenu" ref="tableTabMenuEle" :model="pm.tableTabMenuItems" :popup="true"></p-contextmenu>
		</div>
	</div>
</div>
<#include "../include/page_table.ftl">
<script>
(function(po)
{
	po.currentUserId = "${currentUser.id}";
	
	po.loadSchemaNodes = function()
	{
		var pm = po.vuePageModel();
		var keyword = pm.searchForm.keyword;
		
		pm.loadingSchema = true;
		$.ajaxJson(po.concatContextPath("/schema/queryData"),
		{
			data: { keyword: keyword },
			success: function(response)
			{
				pm.schemaNodes = po.schemasToNodes(response);
			},
			complete: function()
			{
				pm.loadingSchema = false;
			}
		});
	};
	
	po.loadTableNodes = function(schemaNode, page)
	{
		page = (page == null ? 1 : page);
		
		if(!schemaNode)
			return;
		
		var pm = po.vuePageModel();
		var keyword = pm.searchForm.keyword;
		
		pm.loadingSchema = true;
		$.ajaxJson(po.concatContextPath("/schema/"+schemaNode.schemaId+"/pagingQueryTable"),
		{
			data: { keyword: keyword, pageSize: 100, page: page },
			success: function(response)
			{
				var loadedNodes = po.tablePagingDataToNodes(schemaNode.schemaId, response);
				
				if(page > 1)
				{
					var children = schemaNode.children;
					if(children[children.length-1].dataType = "loadMore")
						children.pop();
					
					schemaNode.children = children.concat(loadedNodes);
				}
				else
					schemaNode.children = loadedNodes;
			},
			complete: function()
			{
				pm.loadingSchema = false;
			}
		});
	};
	
	po.evalSchemaNodeForLoadTable = function(schemaNode)
	{
		if(schemaNode != null)
			return schemaNode;
		
		var pm = po.vuePageModel();
		var schemaNodes = (pm.schemaNodes || []);
		var selectedNodeKeys = po.vueRaw(pm.selectedNodeKeys);
		
		if(!selectedNodeKeys)
			return null;
		
		for(var i=0; i<schemaNodes.length; i++)
		{
			var schemaId = schemaNodes[i].schemaId;
			for(var selectedKey in selectedNodeKeys)
			{
				if(selectedKey == schemaId || selectedKey.indexOf(schemaId) == 0)
					return schemaNodes[i];
			}
		}
		
		return null;
	};
	
	po.schemasToNodes = function(schemas)
	{
		var re = [];
		
		schemas.forEach(function(schema)
		{
			var label = schema.title;
			
			if(schema.createUser && schema.createUser.id != po.currentUserId)
				label += " ("+schema.createUser.nameLabel+")";
			
			re.push(
			{
				key: schema.id,
				label: label,
				icon: "pi pi-database",
				leaf: false,
				dataType: "schema",
				schemaId: schema.id
			});
		});
		
		return re;
	};
	
	po.findSchemaNode = function(schemaId)
	{
		var pm = po.vuePageModel();
		var schemaNodes = (pm.schemaNodes || []);
		var idx = $.inArrayById(schemaNodes, schemaId, "schemaId");
		
		return (idx > -1 ? schemaNodes[idx] : null);
	};
	
	po.tablePagingDataToNodes = function(schemaId, pagingData)
	{
		var re = [];
		
		pagingData.items.forEach(function(table)
		{
			re.push(
			{
				key: schemaId + "-" + table.name,
				label: table.name,
				icon: "pi pi-table",
				leaf: true,
				dataType: "table",
				schemaId: schemaId,
				tableName: table.name
			});
		});
		
		//添加下一页节点
		if(pagingData.page < pagingData.pages)
		{
			var showCount = (pagingData.page-1) * pagingData.pageSize + pagingData.items.length;
			
			re.push(
			{
				key: "next-page-for-"+pagingData.page,
				label : "<@spring.message code='loadMore' /> (" + showCount + "/" + pagingData.total+")",
				icon: "pi pi-arrow-down",
				leaf: true,
				styleClass: "font-bold",
				dataType: "loadMore",
				schemaId: schemaId,
				nextPage: pagingData.page + 1
			});
		}
		
		return re;
	};
	
	po.showSchemaTableTab = function(schemaId, tableName)
	{
		var pm = po.vuePageModel();
		
		var idx = po.getSchemaTableTabIndex(schemaId, tableName);
		if(idx > -1)
			pm.tableTabs.activeIndex = idx;
		else
		{
			var tabId = po.schemaTableTabId(schemaId, tableName);
			
			pm.tableTabs.items.push(
			{
				id: tabId,
				title: tableName,
				schemaId: schemaId,
				tableName: tableName
			});
			
			//直接设置activeIndex不会滚动到新加的卡片，所以采用此方案
			po.vueApp().$nextTick(function()
			{
				pm.tableTabs.activeIndex = pm.tableTabs.items.length - 1;
			});
		}
	};
	
	po.loadSchemaTableTab = function(schemaId, tableName)
	{
		var tabId = po.schemaTableTabId(schemaId, tableName);
		var panel = po.elementOfId(tabId);
		
		if(panel.prop("loaded") !== true && panel.prop("loading") !== true)
		{
			panel.prop("loading", true);
			panel.empty();
			
			po.open(po.toSchemaTableUrl(schemaId, tableName),
			{
				target: panel,
				dialog: false,
				success: function()
				{
					panel.prop("loaded", true);
					panel.prop("loading", false);
				}
			});
		}
	};
	
	po.toSchemaTableUrl = function(schemaId, tableName)
	{
		return po.concatContextPath("/data/"+schemaId+"/"+encodeURIComponent(tableName)+"/query");
	};
	
	po.getSchemaTableTabIndex = function(schemaId, tableName)
	{
		var pm = po.vuePageModel();
		var items = pm.tableTabs.items;
		
		return $.inArrayById(items, po.schemaTableTabId(schemaId, tableName));
	};
	
	po.schemaTableTabId = function(schemaId, tableName)
	{
		var map = (po.tableTabIdMap || (po.tableTabIdMap = {}));
		
		//不直接使用tableName作为元素ID，因为name中可能存在与jquery冲突的字符，比如'$'
		var key = (schemaId + tableName);
		var value = map[key];
		
		if(value == null)
		{
			value = $.uid("schemaTableTab");
			map[key] = value;
		}
		
		return value;
	};
	
	po.vuePageModel(
	{
		searchForm:{ keyword: "" },
		searchType: "schema",
		loadingSchema: false,
		schemaNodes: null,
		selectedNodeKeys: null,
		tableTabs:
		{
			items: [],
			activeIndex: 0
		},
		schemaOptItems:
		[
			{
				label: "<@spring.message code='edit' />"
			},
			{
				label: "<@spring.message code='delete' />"
			},
			{
				label: "<@spring.message code='view' />"
			},
			{
				label: "<@spring.message code='refresh' />"
			},
			{
				label: "<@spring.message code='reload' />"
			},
			{
				label: "<@spring.message code='autherization' />"
			},
			{ separator: true },
			{
				label: "<@spring.message code='module.sqlpad' />"
			},
			{
				label: "<@spring.message code='module.importData' />"
			},
			{
				label: "<@spring.message code='module.exportData' />"
			}
		],
		tableTabMenuItems:
		[
			{
				label: "<@spring.message code='close' />"
			},
			{
				label: "<@spring.message code='closeOther' />"
			},
			{
				label: "<@spring.message code='closeRight' />"
			},
			{
				label: "<@spring.message code='closeLeft' />"
			},
			{
				label: "<@spring.message code='closeAll' />"
			}
		]
	});
	
	po.vueRef("tableTabMenuEle", null);
	
	po.vueMethod(
	{
		toSchemaTableUrl: function(tab)
		{
			return po.toSchemaTableUrl(tab.schemaId, tab.tableName);
		},
		onSearch: function()
		{
			var pm = po.vuePageModel();
			
			if(pm.searchType == "schema")
				po.loadSchemaNodes();
			else if(pm.searchType == "table")
			{
				var schemaNode = po.evalSchemaNodeForLoadTable();
				if(!schemaNode)
					$.tipInfo("<@spring.message code='pleaseSelectSchemaForSearchTable' />");
				else
					po.loadTableNodes(schemaNode);
			}
		},
		
		onSchemaNodeExpand: function(node)
		{
			if(node.children == null)
			{
				po.loadTableNodes(node);
			}
		},
		
		onSchemaNodeSelect: function(node)
		{
			if(node.dataType == "loadMore")
			{
				var mySchemaNode = po.findSchemaNode(node.schemaId);
				po.loadTableNodes(mySchemaNode, node.nextPage);
			}
			else if(node.dataType == "table")
			{
				po.showSchemaTableTab(node.schemaId, node.tableName);
			}
		},
		
		onTableTabChange: function(e){},
		
		onTableTabClick: function(e){},
		
		onToggleSearchType: function(e)
		{
			var pm = po.vuePageModel();
			pm.searchType = (pm.searchType == "schema" ? "table" : "schema");
		},
		
		onTableTabMenuToggle: function(e)
		{
			po.vueRef("tableTabMenuEle").show(e);
		},
		
		onAdd: function()
		{
			po.handleAddAction("/schema/add");
		},
		
		onEdit: function()
		{
			po.handleOpenOfAction("/schema/edit");
		},
		
		onView: function()
		{
			po.handleOpenOfAction("/schema/view");
		},
		
		onDelete: function()
		{
			po.handleDeleteAction("/schema/delete");
		}
	});
	
	//po.showSchemaTableTab()里不能里可获取到创建的DOM元素，所以采用此方案
	po.vueWatch(po.vuePageModel().tableTabs, function(oldVal, newVal)
	{
		var items = newVal.items;
		var activeIndex = newVal.activeIndex;
		var activeTab = items[activeIndex];
		
		po.vueApp().$nextTick(function()
		{
			po.loadSchemaTableTab(activeTab.schemaId, activeTab.tableName);
		});
	});
	
	po.vueMounted(function()
	{
		po.loadSchemaNodes();
	});
	
	po.vueMount();
})
(${pid});
</script>
</body>
</html>