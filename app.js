(() => {
  const content = window.WIKI_DATA;
  const $ = id => document.getElementById(id);
  const escape = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const itemUrl = name => window.WIKI_ASSETS?.[name] || (name === 'command_scepter' ? 'assets/renders/command_scepter.svg' : `assets/items/${name}.png`);
  const labels = {
    en:{skip:'Skip to content',fieldGuide:'THE FIELD GUIDE',sections:'Wiki sections',description:'Read the mod description ↗',addon:'A Shape Shifter Curse addon',source:'Source on GitHub ↗',navigation:'Open navigation',wiki:'Wiki',language:'Language',search:'Search the wiki',placeholder:'Search the guide…',adjacent:'Adjacent articles',print:'Print article ↗',contents:'ON THIS PAGE',contentsLabel:'On this page',about:'About this guide',note:'Server settings may change temporary order durations.',close:'Close screenshot',missing:'Article not found',missingText:'Return to the <a href="#home">wiki overview</a> to find a guide.',eyebrow:'MOD DESCRIPTION',table:'Scrollable reference table',explore:'EXPLORE THE GUIDE',open:'Open the wiki →',previous:'← PREVIOUS',next:'NEXT →',back:'BACK TO THE START',overview:'Wiki overview →',articles:'ARTICLES',noMatches:'NO MATCHES',searchHint:'Try “contract”, “apple”, “armor” or “recipe”.'},
    ru:{skip:'Перейти к содержимому',fieldGuide:'РУКОВОДСТВО ПО МОДУ',sections:'Разделы вики',description:'Описание мода ↗',addon:'Аддон для Shape Shifter Curse',source:'Исходники на GitHub ↗',navigation:'Открыть меню',wiki:'Вики',language:'Язык',search:'Поиск по вики',placeholder:'Поиск по руководству…',adjacent:'Соседние статьи',print:'Распечатать статью ↗',contents:'НА ЭТОЙ СТРАНИЦЕ',contentsLabel:'На этой странице',about:'Об этом руководстве',note:'Настройки сервера могут менять срок временных приказов.',close:'Закрыть скриншот',missing:'Статья не найдена',missingText:'Откройте <a href="#home">главную страницу вики</a>, чтобы найти нужное руководство.',eyebrow:'ОПИСАНИЕ МОДА',table:'Справочная таблица с прокруткой',explore:'ОТКРЫТЬ РУКОВОДСТВО',open:'Перейти к вики →',previous:'← НАЗАД',next:'ДАЛЕЕ →',back:'В НАЧАЛО',overview:'Главная страница вики →',articles:'СТАТЬИ',noMatches:'НИЧЕГО НЕ НАЙДЕНО',searchHint:'Попробуйте «договор», «яблоко», «броня» или «рецепт».'}
  };
  let storedLanguage;
  try { storedLanguage = localStorage.getItem('botb-wiki-language'); } catch {}
  let language = new URLSearchParams(location.search).get('lang') || storedLanguage || 'en';
  if(!content[language]) language='en';
  let pages, byId, ui, currentPage;
  const closeMenu = () => { document.body.classList.remove('menu-open'); $('menu-button').setAttribute('aria-expanded','false'); };
  $('menu-button').addEventListener('click',()=>{const open=document.body.classList.toggle('menu-open');$('menu-button').setAttribute('aria-expanded',String(open));});
  $('shade').addEventListener('click',closeMenu);
  document.querySelector('.skip').addEventListener('click',event=>{event.preventDefault();$('main-content').focus();$('main-content').scrollIntoView();});
  function applyLanguage(){
    pages=content[language]; byId=new Map(pages.map(page=>[page.id,page])); ui=labels[language]; currentPage=null;
    document.documentElement.lang=language;
    document.querySelectorAll('[data-i18n]').forEach(el=>el.textContent=ui[el.dataset.i18n]);
    document.querySelectorAll('[data-i18n-label]').forEach(el=>el.setAttribute('aria-label',ui[el.dataset.i18nLabel]));
    document.querySelectorAll('[data-language]').forEach(button=>{button.setAttribute('aria-pressed',String(button.dataset.language===language));});
    $('search').placeholder=ui.placeholder;
    const groups=[...new Set(pages.filter(p=>p.id!=='description').map(p=>p.group))];
    $('navigation').innerHTML=groups.map(group=>`<div class="nav-group"><p>${escape(group)}</p>${pages.filter(p=>p.group===group&&p.id!=='description').map(p=>`<a href="#${p.id}" data-page="${p.id}"><span class="nav-mark" aria-hidden="true"></span>${escape(p.title)}</a>`).join('')}</div>`).join('');
  }
  document.querySelectorAll('[data-language]').forEach(button=>button.addEventListener('click',()=>{
    if(language===button.dataset.language)return;
    language=button.dataset.language;
    try {localStorage.setItem('botb-wiki-language',language);}catch{}
    try {const url=new URL(location.href);url.searchParams.set('lang',language);history.replaceState(null,'',url);}catch{}
    applyLanguage();render();
  }));
  function render(){
    const [id='home',anchor]=(location.hash.slice(1)||'home').split(':');
    if(id==='installation'){location.hash='home';return;}
    const page=byId.get(id);
    if(!page){currentPage=null;$('page-content').innerHTML=`<h1>${ui.missing}</h1><p>${ui.missingText}</p>`;$('contents-links').innerHTML='';$('page-turns').innerHTML='';$('page-eyebrow').textContent=ui.wiki;$('breadcrumb-title').textContent=ui.missing;closeMenu();return;}
    const changed=currentPage!==id||!$('page-content').children.length;
    currentPage=id;document.body.dataset.page=id;
    if(changed){
      $('page-content').innerHTML=page.html;
      $('page-eyebrow').innerHTML=`<img src="${itemUrl(page.icon)}" width="25" height="25" alt=""><span>${escape(page.id==='description'?ui.eyebrow:page.group)}</span>`;
      $('breadcrumb-title').textContent=page.title;
      document.title=`${page.title} · Bond of the Beast`;
      document.querySelectorAll('[data-page]').forEach(link=>{const active=link.dataset.page===id;link.classList.toggle('active',active);if(active)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
      const headings=[...$('page-content').querySelectorAll('h2,h3')];
      $('contents-links').innerHTML=headings.filter(h=>h.tagName==='H2').map(h=>`<a href="#${id}:${h.id}">${escape(h.textContent)}</a>`).join('');
      $('page-content').querySelectorAll('table').forEach(table=>{const wrap=document.createElement('div');wrap.className='table-scroll';wrap.tabIndex=0;wrap.setAttribute('role','region');wrap.setAttribute('aria-label',ui.table);table.before(wrap);wrap.append(table);});
      const wiki=pages.filter(p=>p.id!=='description'),i=wiki.findIndex(p=>p.id===id),previous=wiki[i-1],next=wiki[i+1];
      $('page-turns').innerHTML=id==='description'?`<a class="next-page" href="#home"><small>${ui.explore}</small><strong>${ui.open}</strong></a>`:`${previous?`<a href="#${previous.id}"><small>${ui.previous}</small><strong>${escape(previous.title)}</strong></a>`:'<span></span>'}${next?`<a class="next-page" href="#${next.id}"><small>${ui.next}</small><strong>${escape(next.title)}</strong></a>`:`<a class="next-page" href="#home"><small>${ui.back}</small><strong>${ui.overview}</strong></a>`}`;
      if(!anchor)window.scrollTo({top:0,left:0,behavior:'instant'});
    }
    if(anchor)requestAnimationFrame(()=>document.getElementById(anchor)?.scrollIntoView({block:'start'}));
    closeMenu();hideSearch();
  }
  const search=$('search'),results=$('search-results');
  function hideSearch(){results.hidden=true;search.setAttribute('aria-expanded','false');}
  function runSearch(){
    const query=search.value.trim().toLocaleLowerCase(language);
    if(!query){hideSearch();return;}
    const words=query.split(/\s+/);
    const matches=pages.map(page=>{const title=page.title.toLocaleLowerCase(language),body=page.text.toLocaleLowerCase(language);return{page,score:words.every(w=>body.includes(w)||title.includes(w))?words.reduce((n,w)=>n+(title.includes(w)?8:0)+(page.summary.toLocaleLowerCase(language).includes(w)?3:1),0):0};}).filter(r=>r.score>0).sort((a,b)=>b.score-a.score).slice(0,7);
    results.innerHTML=`<div class="search-caption">${matches.length?ui.articles:ui.noMatches}</div>`+(matches.length?matches.map(({page})=>{const pos=Math.max(0,page.text.toLocaleLowerCase(language).indexOf(words[0])-35),snippet=(pos?'…':'')+page.text.slice(pos,pos+135)+'…';return`<a href="#${page.id}"><strong>${escape(page.title)}</strong><span>${escape(snippet)}</span></a>`;}).join(''):`<p>${ui.searchHint}</p>`);
    results.hidden=false;search.setAttribute('aria-expanded','true');
  }
  search.addEventListener('input',runSearch);
  search.addEventListener('focus',()=>{if(search.value.trim())runSearch();});
  search.addEventListener('keydown',event=>{if(event.key==='Escape')hideSearch();if(event.key==='Enter'){const first=results.querySelector('a');if(first){location.hash=first.hash;hideSearch();search.blur();}}if(event.key==='ArrowDown'){event.preventDefault();results.querySelector('a')?.focus();}});
  results.addEventListener('click',event=>{if(event.target.closest('a'))hideSearch();});
  document.addEventListener('click',event=>{if(!event.target.closest('.search-wrap'))hideSearch();});
  document.addEventListener('keydown',event=>{if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==='k'){event.preventDefault();search.focus();search.select();}if(event.key==='Escape'){closeMenu();hideSearch();}});
  $('print-button').addEventListener('click',()=>window.print());
  window.addEventListener('hashchange',render);
  applyLanguage();render();
})();
