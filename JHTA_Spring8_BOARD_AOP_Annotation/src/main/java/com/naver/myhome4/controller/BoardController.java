package com.naver.myhome4.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.naver.myhome4.domain.Board;
import com.naver.myhome4.domain.Comment;
import com.naver.myhome4.service.BoardService;
import com.naver.myhome4.service.CommentService;

@Controller
@RequestMapping(value = "/board")
public class BoardController {

	private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

	@Autowired
	private BoardService boardService;

	@Autowired
	private CommentService commentService;
	
	@Value("${savefoldername}")
	private String saveFolder;

	@GetMapping(value = "/write")
	public String write() {
		return "board/board_write";
	}

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ModelAndView boardList(@RequestParam(value = "page", defaultValue = "1", required = false) int page,
			ModelAndView mv) {

		int limit = 10;
		int listcount = boardService.getListCount(); // 총 리스트 수를 받아옴
		int maxpage = (listcount + limit - 1) / limit;
		int startpage = ((page - 1) / 10) * 10 + 1;
		int endpage = startpage + 10 - 1;
		if (endpage > maxpage) {
			endpage = maxpage;
		}

		List<Board> boardlist = boardService.getBoardList(page, limit);

		mv.setViewName("board/board_list");
		mv.addObject("page", page);
		mv.addObject("maxpage", maxpage);
		mv.addObject("startpage", startpage);
		mv.addObject("endpage", endpage);
		mv.addObject("listcount", listcount);
		mv.addObject("boardlist", boardlist);
		mv.addObject("limit", limit);
		return mv;
	}

	@ResponseBody
	@RequestMapping(value = "/list_ajax", method = RequestMethod.POST)
	public Map<String, Object> boardListAjax(
			@RequestParam(value = "page", defaultValue = "1", required = false) int page,
			@RequestParam(value = "limit", defaultValue = "10", required = false) int limit) {

		int listcount = boardService.getListCount(); // 총 리스트 수를 받아옴
		int maxpage = (listcount + limit - 1) / limit;
		int startpage = ((page - 1) / 10) * 10 + 1;
		int endpage = startpage + 10 - 1;
		if (endpage > maxpage) {
			endpage = maxpage;
		}

		List<Board> boardlist = boardService.getBoardList(page, limit); // 리스트를 받아옴

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("page", page);
		map.put("maxpage", maxpage);
		map.put("startpage", startpage);
		map.put("endpage", endpage);
		map.put("listcount", listcount);
		map.put("boardlist", boardlist);
		map.put("limit", limit);
		return map;
	}

	@PostMapping(value = "/add")
	public String add(Board board) throws Exception {

		MultipartFile uploadfile = board.getUploadfile();

		if (!uploadfile.isEmpty()) {
			String fileName = uploadfile.getOriginalFilename(); // 원래 파일명
			board.setBoard_original(fileName); // 원래 파일명 저장
//			String saveFolder = request.getSession().getServletContext().getRealPath("resources") + "/upload/";
			String fileDBName = fileDBName(fileName, saveFolder);
			logger.info("fileDBName = " + fileDBName);

			// transferTo(File path) : 업로드한 파일을 매개변수의 경로에 저장합니다.
			uploadfile.transferTo(new File(saveFolder + fileDBName));

			// 바뀐 파일명으로 저장
			board.setBoard_file(fileDBName);
		}

		boardService.insertBoard(board); // 저장메서드 호출

		return "redirect:list";
	}

	private String fileDBName(String fileName, String saveFolder) {

		// 새로운 폴더 이름 : 오늘 + 년 + 월 + 일
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH)+1;
		int date = c.get(Calendar.DATE);

		String homedir = saveFolder + year + "-" + month + "-" + date;
		logger.info(homedir);
		File path1 = new File(homedir);
		if (!(path1.exists())) {
			path1.mkdir(); // 새로운 폴더를 생성
		}

		// 난수를 구합니다.
		Random r = new Random();
		int random = r.nextInt(100000000);

		// 확장자 구하기 시작
		int index = fileName.lastIndexOf(".");
		// 문자열에서 특정 문자열의 위치값(index)를 반환
		// indexOf가 처음 발견되는 문자열에 대한 index를 반환하는 반면,
		// lastIndexOf는 마지막으로 반결되는 문자열의 index를 반환합니다.
		// (파일명에 점이 여러개 있을 경우 맨 마지막에 발견되는 문자열의 위치를 리턴합니다.)
		logger.info("index = " + index);
		String fileExtension = fileName.substring(index + 1);
		logger.info("fileExtension = " + fileExtension);
		// 확장자 구하기 끝

		// 새로운 파일명
		String refileName = "bbs" + year + month + date + random + "." + fileExtension;
		logger.info("refileName = " + refileName);

		// 오라클 디비에 저장될 파일명
		String fileDBName = "/" + year + "-" + month + "-" + date + "/" + refileName;
		logger.info("fileDBName = " + fileDBName);

		return fileDBName;
	}

	@GetMapping("/detail")
	public ModelAndView detail(int num, ModelAndView mv, HttpServletRequest request) {
		Board board = boardService.getDetail(num);
		if (board == null) {
			logger.info("상세보기 실패");
			mv.setViewName("error/error");
			mv.addObject("url", request.getRequestURL());
			mv.addObject("message", "상세보기 실패입니다.");
		} else {
			logger.info("상세보기 성공");
			int count = commentService.getListCount(num);
			mv.setViewName("board/board_view");
			mv.addObject("count", count);
			mv.addObject("boarddata", board);
		}
		return mv;
	}

	@GetMapping("/modifyView")
	public ModelAndView modifyView(int num, ModelAndView mv, HttpServletRequest request) {
		Board boarddata = boardService.getDetail(num);

		if (boarddata == null) {
			logger.info("수정보기 실패");
			mv.setViewName("error/error");
			mv.addObject("url", request.getRequestURL());
			mv.addObject("message", "수정보기 실패입니다.");
			return mv;
		}
		logger.info("(수정) 상세보기 성공");
		// 수정 폼 페이지로 이동할 때 원문 글 내용을 보여주기 때문에 boarddata 객체를
		// ModelAndView 객체에 저장합니다.
		mv.addObject("boarddata", boarddata);
		// 글 수정 폼 페이지로 이동하기 위해 경로를 설정합니다.
		mv.setViewName("board/board_modify");
		return mv;
	}

	@PostMapping("/modifyAction")
	public String BoardModifyAction(Board boarddata, String check, Model mv, HttpServletRequest request,
			RedirectAttributes rattr) throws Exception {
		
		// <input type="hidden" name="BOARD_FILE" value="${baorddata.BOARD_FILE}">
		// baorddata.getBoard_file()는 위 문장의 값을 가져옵니다. 즉, 이전에 선택한 파일 이름입니다. 
		String before_file = boarddata.getBoard_file();
		
		boolean usercheck = boardService.isBoardWriter(boarddata.getBoard_num(), boarddata.getBoard_pass());
		String url = "";

		// 비밀번호가 다른경우
		if (usercheck == false) {
			rattr.addFlashAttribute("result", "passFail");
			rattr.addAttribute("num", boarddata.getBoard_num());
			return "redirect:modifyView";
		}

		MultipartFile uploadfile = boarddata.getUploadfile();
//		String saveFolder = request.getSession().getServletContext().getRealPath("resources") + "/upload/";

		if (check != null && !check.equals("")) { // 기존 파일 그대로 사용하는 경우
			logger.info("기존파일 그래도 사용합니다.");
			boarddata.setBoard_original(check);

		} else {
			if (uploadfile!=null && !uploadfile.isEmpty()) {
				logger.info("파일 변경되었습니다.");

				String fileName = uploadfile.getOriginalFilename(); // 원래 파일명 
				
				boarddata.setBoard_original(fileName);

				String fileDBName = fileDBName(fileName, saveFolder);

				// transferTo(File path) : 업로드한 파일을 매개변수의 경로에 저장합니다.
				uploadfile.transferTo(new File(saveFolder + fileDBName));

				// 바뀐 파일명으로 저장
				boarddata.setBoard_file(fileDBName);
			} else { // uploadfile.isEmpty() 인 경우 - 파일을 선택하지 않은 경
				logger.info("선택 파일 없습니다.");
				boarddata.setBoard_file(""); // ""로 초기화 합니다.
				boarddata.setBoard_original("");// ""로 초기화 합니다.
			}
		}

		// DAO에서 수정 메서드 호출하여 수정합니다.
		int result = boardService.boardModify(boarddata);
		// 수정에 실패한 경우
		if (result == 0) {
			logger.info("게시판 수정 실패");
			mv.addAttribute("url", request.getRequestURL());
			mv.addAttribute("message", "게시판 수정 실패");
			url = "error/error";
		} else { // 수정 성공
			logger.info("게시판 수정 완료");
			// 수정한 글 내용을 보여주기 위한 글 내용 보기 페이지로 이동하기 위해 경로를 설정
			url = "redirect:detail";
			rattr.addAttribute("num", boarddata.getBoard_num());
			
			// 파일 삭제를 위해 추가한 부분 
			// 수정 전에 파일이 있고 이전 파일명과 현재 파일명이 다른 경우 삭제할 목록을 테이블에 추가 
			if(!before_file.equals("") && !before_file.equals(boarddata.getBoard_file())) {
				// 원래 파일명을 DB에 있는 삭제할 파일목록에 저장 
				boardService.insertDeleteFiles(before_file);
			}
		}
		return url;
	}

	@GetMapping(value = "/replyView")
	public ModelAndView replyView(int num, ModelAndView mv, HttpServletRequest request) {
		Board boarddata = boardService.getDetail(num);
		if (boarddata == null) {
			mv.setViewName("error/error");
			mv.addObject("url", request.getRequestURL());
			mv.addObject("message", "게시판 답변글 가져오기 실패");
		} else {
			mv.addObject("boarddata", boarddata);
			mv.setViewName("board/board_reply");
		}
		return mv;
	}
	
	@PostMapping("/replyAction")
	public ModelAndView replyAction(Board board, ModelAndView mv, HttpServletRequest request) {
		int result = boardService.boardReply(board);
		if(result == 0) {
			mv.setViewName("error/error");
			mv.addObject("url", request.getRequestURL());
			mv.addObject("message", "게시판 답변 처리 실패");
		} else {
			mv.setViewName("redirect:list");
		}
		return mv;
	}
	
	@PostMapping("/delete")
	public String boardDeleteAction(String board_pass, int num, Model mv, RedirectAttributes rattr, HttpServletRequest request) throws Exception {
		boolean usercheck = boardService.isBoardWriter(num, board_pass);
		
		// 비밀번호 일치하지 않는 경우
		if(usercheck == false) {
			rattr.addFlashAttribute("result", "passFail");
			rattr.addAttribute("num", num);
			return "redirect:detail";
		}
		
		// 비밀번호 일치하는 경우 삭제 처리
		int result = boardService.boardDelete(num);
		
		// 삭제 처리 실패한 경우
		if(result == 0) {
			logger.info("게시판 삭제 실패");
			mv.addAttribute("url", request.getRequestURL());
			mv.addAttribute("message", "삭제 실패");
			return "error/error";
		}
		
		// 삭제 처리 성공한 경우 - 글 목록 보기 요청을 전송하는 부분
		logger.info("게시판 삭제 성공");
		rattr.addFlashAttribute("result", "deleteSuccess");
		return "redirect:list";
	}
	
	@GetMapping(value="/down", produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> boardFileDown(String original, String filename, HttpServletRequest request) {
		// 파일이 저정되는 실제 경로를 가져와서 저장 
//		String saveFolder = request.getSession().getServletContext().getRealPath("resources") + "/upload/";
		logger.info(saveFolder);
		// 저장된 경로와 파일 이름을 조합하여 파일주소를 만들어줌
		Resource resource = new FileSystemResource(saveFolder + filename);
	
		// 파일이 없는 경우 "NOT FOUND"를 리턴 
		if (resource.exists() == false) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		HttpHeaders headers = new HttpHeaders();
		
		try {
			String downloadName = new String(original.getBytes("UTF-8"), "ISO-8859-1");
			headers.add("Content-Disposition", "attachment; filename=" + downloadName);
		} catch(UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
	}
}
