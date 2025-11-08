package com.ecommerce.questions.domain.repository;

/** Repositorio de Question */


//Spring
import org.springframework.data.jpa.repository.JpaRepository;

//Dominio
import com.ecommerce.questions.domain.model.Question;

public interface QuestionRepository extends JpaRepository <Question,String>{

}
